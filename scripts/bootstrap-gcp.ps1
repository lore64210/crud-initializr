#Requires -Version 5.1
<#
.SYNOPSIS
    Configura desde cero el backend de crud-initializr en GCP para desplegar
    con GitHub Actions: APIs, service accounts, Artifact Registry, IAM y
    Workload Identity Federation.

.DESCRIPTION
    Versión PowerShell del script scripts/bootstrap-gcp.sh, para ejecutar
    desde Windows con gcloud autenticado. Es idempotente.

.EXAMPLE
    .\scripts\bootstrap-gcp.ps1 -ProjectId spring-crud-initializr `
        -GitHubRepo lore64210/crud-initializr
#>
[CmdletBinding()]
param(
    [string]$ProjectId = $env:PROJECT_ID,
    [string]$Region = $(if ($env:REGION) { $env:REGION } else { "us-central1" }),
    [string]$GarRepository = $(if ($env:GAR_REPOSITORY) { $env:GAR_REPOSITORY } else { "crud-initializr" }),
    [string]$Service = $(if ($env:SERVICE) { $env:SERVICE } else { "crud-initializr" }),
    [string]$GitHubRepo = $(if ($env:GITHUB_REPO) { $env:GITHUB_REPO } else { "lore64210/crud-initializr" }),
    [string]$DeploySaName = "github-deployer",
    [string]$RuntimeSaName = "crud-run-sa",
    [string]$Pool = "github-pool",
    [string]$Provider = "github",
    [string]$Branch = "main"
)

$ErrorActionPreference = "Continue"

function Invoke-Gcloud {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    & gcloud.cmd @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "gcloud falló: gcloud $($Arguments -join ' ')"
    }
}

function Test-Gcloud {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    & gcloud.cmd @Arguments *> $null
    return $LASTEXITCODE -eq 0
}

if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw "gcloud no está en PATH. Instalá Google Cloud CLI o usá Cloud Shell."
}

$activeAccounts = & gcloud.cmd auth list --filter=status:ACTIVE --format="value(account)"
if (-not $activeAccounts) {
    throw "No hay una cuenta activa en gcloud. Ejecutá: gcloud auth login"
}

if (-not $ProjectId) {
    $ProjectId = (& gcloud.cmd config get-value project 2>$null)
    if ($ProjectId -eq "(unset)") { $ProjectId = "" }
}
if (-not $ProjectId) {
    throw "Definí -ProjectId. Ejemplo: .\scripts\bootstrap-gcp.ps1 -ProjectId mi-proyecto"
}

Invoke-Gcloud config set project $ProjectId | Out-Null

Write-Host "==> Proyecto: $ProjectId"
Write-Host "==> Región: $Region"
Write-Host "==> Repositorio GitHub: $GitHubRepo (rama $Branch)"

Write-Host "==> Habilitando APIs"
Invoke-Gcloud services enable `
    run.googleapis.com `
    artifactregistry.googleapis.com `
    iam.googleapis.com `
    iamcredentials.googleapis.com `
    cloudresourcemanager.googleapis.com `
    serviceusage.googleapis.com `
    cloudbilling.googleapis.com `
    billingbudgets.googleapis.com `
    --project=$ProjectId | Out-Null

$projectNumber = (& gcloud.cmd projects describe $ProjectId --format="value(projectNumber)").Trim()
$deploySaEmail = "$DeploySaName@$ProjectId.iam.gserviceaccount.com"
$runtimeSaEmail = "$RuntimeSaName@$ProjectId.iam.gserviceaccount.com"

function Ensure-ServiceAccount {
    param(
        [string]$Name,
        [string]$Email,
        [string]$DisplayName
    )

    if (Test-Gcloud iam service-accounts describe $Email --project=$ProjectId) {
        Write-Host "==> Service account ya existe: $Email"
    }
    else {
        Write-Host "==> Creando service account: $Email"
        Invoke-Gcloud iam service-accounts create $Name --display-name=$DisplayName --project=$ProjectId | Out-Null
    }
}

Ensure-ServiceAccount -Name $DeploySaName -Email $deploySaEmail -DisplayName "GitHub Actions deployer"
Ensure-ServiceAccount -Name $RuntimeSaName -Email $runtimeSaEmail -DisplayName "Cloud Run runtime"

Write-Host "==> Creando repositorio de Artifact Registry"
if (Test-Gcloud artifacts repositories describe $GarRepository --location=$Region --project=$ProjectId) {
    Write-Host "==> Repositorio ya existe: $GarRepository"
}
else {
    Invoke-Gcloud artifacts repositories create $GarRepository `
        --repository-format=docker `
        --location=$Region `
        --description="Imágenes Docker de $Service" `
        --project=$ProjectId | Out-Null
}

Write-Host "==> Aplicando retención de imágenes (máximo 2 versiones)"
$policyFile = [System.IO.Path]::GetTempFileName()
$policy = @'
[
  {
    "name": "keep-2-recent",
    "action": { "type": "Keep" },
    "mostRecentVersions": { "keepCount": 2 }
  },
  {
    "name": "delete-rest",
    "action": { "type": "Delete" },
    "condition": { "tagState": "ANY" }
  }
]
'@
try {
    Set-Content -Path $policyFile -Value $policy -Encoding ASCII
    Invoke-Gcloud artifacts repositories set-cleanup-policies $GarRepository `
        --location=$Region `
        --policy=$policyFile `
        --no-dry-run `
        --project=$ProjectId | Out-Null
}
finally {
    Remove-Item $policyFile -Force -ErrorAction SilentlyContinue
}

Write-Host "==> Asignando roles al deployer de GitHub Actions"
$deployerRoles = @(
    "roles/run.admin",
    "roles/artifactregistry.writer",
    "roles/serviceusage.serviceUsageConsumer"
)
foreach ($role in $deployerRoles) {
    Invoke-Gcloud projects add-iam-policy-binding $ProjectId `
        --member="serviceAccount:$deploySaEmail" `
        --role=$role `
        --condition=None `
        --quiet | Out-Null
}

Invoke-Gcloud iam service-accounts add-iam-policy-binding $runtimeSaEmail `
    --member="serviceAccount:$deploySaEmail" `
    --role="roles/iam.serviceAccountUser" `
    --condition=None `
    --project=$ProjectId `
    --quiet | Out-Null

Write-Host "==> Permitiendo que Cloud Run lea las imágenes del repositorio"
$serviceAgent = "service-$projectNumber@serverless-robot-prod.iam.gserviceaccount.com"
try {
    Invoke-Gcloud projects add-iam-policy-binding $ProjectId `
        --member="serviceAccount:$serviceAgent" `
        --role="roles/artifactregistry.reader" `
        --condition=None `
        --quiet | Out-Null
}
catch {
    Write-Warning "No se pudo asignar Artifact Registry Reader al service agent de Cloud Run."
    Write-Warning "Si el deploy falla al bajar la imagen, asignalo manualmente en IAM."
}

Write-Host "==> Configurando Workload Identity Federation para GitHub Actions"
if (-not (Test-Gcloud iam workload-identity-pools describe $Pool --location=global --project=$ProjectId)) {
    Invoke-Gcloud iam workload-identity-pools create $Pool `
        --location=global `
        --display-name="GitHub Actions" `
        --project=$ProjectId | Out-Null
}

$attributeMapping = "google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.repository=assertion.repository,attribute.ref=assertion.ref"
$attributeCondition = "assertion.repository == '$GitHubRepo' && assertion.ref == 'refs/heads/$Branch'"

if (Test-Gcloud iam workload-identity-pools providers describe $Provider --location=global --workload-identity-pool=$Pool --project=$ProjectId) {
    Invoke-Gcloud iam workload-identity-pools providers update-oidc $Provider `
        --location=global `
        --workload-identity-pool=$Pool `
        --issuer-uri="https://token.actions.githubusercontent.com" `
        --attribute-mapping=$attributeMapping `
        --attribute-condition=$attributeCondition `
        --project=$ProjectId | Out-Null
}
else {
    Invoke-Gcloud iam workload-identity-pools providers create-oidc $Provider `
        --location=global `
        --workload-identity-pool=$Pool `
        --display-name="GitHub" `
        --issuer-uri="https://token.actions.githubusercontent.com" `
        --attribute-mapping=$attributeMapping `
        --attribute-condition=$attributeCondition `
        --project=$ProjectId | Out-Null
}

$wifMember = "principalSet://iam.googleapis.com/projects/$projectNumber/locations/global/workloadIdentityPools/$Pool/attribute.repository/$GitHubRepo"
Invoke-Gcloud iam service-accounts add-iam-policy-binding $deploySaEmail `
    --member=$wifMember `
    --role="roles/iam.workloadIdentityUser" `
    --condition=None `
    --project=$ProjectId `
    --quiet | Out-Null

$wifProvider = "projects/$projectNumber/locations/global/workloadIdentityPools/$Pool/providers/$Provider"

Write-Host @"

============================================================
GCP quedó configurado.
============================================================

Variables para GitHub:

  gh variable set GCP_PROJECT_ID --body "$ProjectId" --repo "$GitHubRepo"
  gh variable set GCP_REGION --body "$Region" --repo "$GitHubRepo"
  gh variable set GAR_REPOSITORY --body "$GarRepository" --repo "$GitHubRepo"
  gh variable set CLOUD_RUN_SERVICE --body "$Service" --repo "$GitHubRepo"
  gh variable set GCP_WIF_PROVIDER --body "$wifProvider" --repo "$GitHubRepo"
  gh variable set GCP_DEPLOY_SA --body "$deploySaEmail" --repo "$GitHubRepo"
  gh variable set GCP_RUNTIME_SA --body "$runtimeSaEmail" --repo "$GitHubRepo"

Después del primer deploy del backend:

  gh variable set API_URL --body "https://<url-de-cloud-run>" --repo "$GitHubRepo"
  gh variable set CLOUDFLARE_PAGES_PROJECT --body "crud-initializr" --repo "$GitHubRepo"

Secretos de Cloudflare:

  gh secret set CLOUDFLARE_API_TOKEN --repo "$GitHubRepo"
  gh secret set CLOUDFLARE_ACCOUNT_ID --repo "$GitHubRepo"

Workload Identity Provider:

  $wifProvider

Service accounts:

  deploy: $deploySaEmail
  runtime: $runtimeSaEmail
"@
