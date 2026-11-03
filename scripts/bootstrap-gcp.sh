#!/usr/bin/env bash
#
# Configura desde cero el backend de crud-initializr en GCP para desplegar
# con GitHub Actions: APIs, service accounts, Artifact Registry, IAM y
# Workload Identity Federation.
#
# Uso recomendado (Cloud Shell):
#   PROJECT_ID=mi-proyecto \
#   GITHUB_REPO=lore64210/crud-initializr \
#   bash scripts/bootstrap-gcp.sh
#
# Variables opcionales:
#   REGION=us-central1
#   GAR_REPOSITORY=crud-initializr
#   SERVICE=crud-initializr
#   BRANCH=main
#
set -euo pipefail

PROJECT_ID="${PROJECT_ID:-}"
REGION="${REGION:-us-central1}"
GAR_REPOSITORY="${GAR_REPOSITORY:-crud-initializr}"
SERVICE="${SERVICE:-crud-initializr}"
GITHUB_REPO="${GITHUB_REPO:-lore64210/crud-initializr}"
DEPLOY_SA_NAME="${DEPLOY_SA_NAME:-github-deployer}"
RUNTIME_SA_NAME="${RUNTIME_SA_NAME:-crud-run-sa}"
POOL="${POOL:-github-pool}"
PROVIDER="${PROVIDER:-github}"
BRANCH="${BRANCH:-main}"

if ! command -v gcloud >/dev/null 2>&1; then
  echo "ERROR: gcloud no está instalado. Usá Google Cloud Shell o instalá el SDK." >&2
  exit 1
fi

if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q .; then
  echo "ERROR: no hay una cuenta activa en gcloud. Ejecutá: gcloud auth login" >&2
  exit 1
fi

if [[ -z "$PROJECT_ID" ]]; then
  PROJECT_ID="$(gcloud config get-value project 2>/dev/null || true)"
fi

if [[ -z "$PROJECT_ID" || "$PROJECT_ID" == "(unset)" ]]; then
  echo "ERROR: definí PROJECT_ID. Ejemplo:" >&2
  echo "  PROJECT_ID=mi-proyecto GITHUB_REPO=lore64210/crud-initializr bash scripts/bootstrap-gcp.sh" >&2
  exit 1
fi

gcloud config set project "$PROJECT_ID" >/dev/null

echo "==> Proyecto: $PROJECT_ID"
echo "==> Región: $REGION"
echo "==> Repositorio GitHub: $GITHUB_REPO (rama $BRANCH)"

echo "==> Habilitando APIs"
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  iam.googleapis.com \
  iamcredentials.googleapis.com \
  cloudresourcemanager.googleapis.com \
  serviceusage.googleapis.com \
  cloudbilling.googleapis.com \
  billingbudgets.googleapis.com \
  --project="$PROJECT_ID"

PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
DEPLOY_SA_EMAIL="${DEPLOY_SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
RUNTIME_SA_EMAIL="${RUNTIME_SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

ensure_service_account() {
  local name="$1"
  local email="$2"
  local display="$3"

  if gcloud iam service-accounts describe "$email" --project="$PROJECT_ID" >/dev/null 2>&1; then
    echo "==> Service account ya existe: $email"
  else
    echo "==> Creando service account: $email"
    gcloud iam service-accounts create "$name" \
      --display-name="$display" \
      --project="$PROJECT_ID"
  fi
}

ensure_service_account "$DEPLOY_SA_NAME" "$DEPLOY_SA_EMAIL" "GitHub Actions deployer"
ensure_service_account "$RUNTIME_SA_NAME" "$RUNTIME_SA_EMAIL" "Cloud Run runtime"

echo "==> Creando repositorio de Artifact Registry"
if gcloud artifacts repositories describe "$GAR_REPOSITORY" \
  --location="$REGION" \
  --project="$PROJECT_ID" >/dev/null 2>&1; then
  echo "==> Repositorio ya existe: $GAR_REPOSITORY"
else
  gcloud artifacts repositories create "$GAR_REPOSITORY" \
    --repository-format=docker \
    --location="$REGION" \
    --description="Imágenes Docker de $SERVICE" \
    --project="$PROJECT_ID"
fi

echo "==> Aplicando retención de imágenes (máximo 2 versiones)"
POLICY_FILE="$(mktemp)"
trap 'rm -f "$POLICY_FILE"' EXIT
cat > "$POLICY_FILE" <<'JSON'
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
JSON
gcloud artifacts repositories set-cleanup-policies "$GAR_REPOSITORY" \
  --location="$REGION" \
  --policy="$POLICY_FILE" \
  --project="$PROJECT_ID"

echo "==> Asignando roles al deployer de GitHub Actions"
for role in \
  roles/run.admin \
  roles/artifactregistry.writer \
  roles/serviceusage.serviceUsageConsumer; do
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$DEPLOY_SA_EMAIL" \
    --role="$role" \
    --condition=None \
    --quiet >/dev/null
done

gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_SA_EMAIL" \
  --member="serviceAccount:$DEPLOY_SA_EMAIL" \
  --role="roles/iam.serviceAccountUser" \
  --condition=None \
  --project="$PROJECT_ID" \
  --quiet >/dev/null

echo "==> Permitiendo que Cloud Run lea las imágenes del repositorio"
if ! gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:service-${PROJECT_NUMBER}@serverless-robot-prod.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.reader" \
  --condition=None \
  --quiet >/dev/null 2>&1; then
  echo "   Aviso: no se pudo asignar Artifact Registry Reader al service agent de Cloud Run."
  echo "   Si el deploy falla al bajar la imagen, asignalo manualmente en IAM."
fi

echo "==> Configurando Workload Identity Federation para GitHub Actions"
if ! gcloud iam workload-identity-pools describe "$POOL" \
  --location=global \
  --project="$PROJECT_ID" >/dev/null 2>&1; then
  gcloud iam workload-identity-pools create "$POOL" \
    --location=global \
    --display-name="GitHub Actions" \
    --project="$PROJECT_ID"
fi

ATTRIBUTE_MAPPING="google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.repository=assertion.repository,attribute.ref=assertion.ref"
ATTRIBUTE_CONDITION="assertion.repository == '${GITHUB_REPO}' && assertion.ref == 'refs/heads/${BRANCH}'"

if gcloud iam workload-identity-pools providers describe "$PROVIDER" \
  --location=global \
  --workload-identity-pool="$POOL" \
  --project="$PROJECT_ID" >/dev/null 2>&1; then
  gcloud iam workload-identity-pools providers update-oidc "$PROVIDER" \
    --location=global \
    --workload-identity-pool="$POOL" \
    --issuer-uri="https://token.actions.githubusercontent.com" \
    --attribute-mapping="$ATTRIBUTE_MAPPING" \
    --attribute-condition="$ATTRIBUTE_CONDITION" \
    --project="$PROJECT_ID"
else
  gcloud iam workload-identity-pools providers create-oidc "$PROVIDER" \
    --location=global \
    --workload-identity-pool="$POOL" \
    --display-name="GitHub" \
    --issuer-uri="https://token.actions.githubusercontent.com" \
    --attribute-mapping="$ATTRIBUTE_MAPPING" \
    --attribute-condition="$ATTRIBUTE_CONDITION" \
    --project="$PROJECT_ID"
fi

WIF_MEMBER="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL}/attribute.repository/${GITHUB_REPO}"

gcloud iam service-accounts add-iam-policy-binding "$DEPLOY_SA_EMAIL" \
  --member="$WIF_MEMBER" \
  --role="roles/iam.workloadIdentityUser" \
  --condition=None \
  --project="$PROJECT_ID" \
  --quiet >/dev/null

WIF_PROVIDER="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL}/providers/${PROVIDER}"

cat <<EOF

============================================================
GCP quedó configurado.
============================================================

Configurá estas variables en GitHub (con gh o desde Settings > Variables):

  gh variable set GCP_PROJECT_ID --body "$PROJECT_ID" --repo "$GITHUB_REPO"
  gh variable set GCP_REGION --body "$REGION" --repo "$GITHUB_REPO"
  gh variable set GAR_REPOSITORY --body "$GAR_REPOSITORY" --repo "$GITHUB_REPO"
  gh variable set CLOUD_RUN_SERVICE --body "$SERVICE" --repo "$GITHUB_REPO"
  gh variable set GCP_WIF_PROVIDER --body "$WIF_PROVIDER" --repo "$GITHUB_REPO"
  gh variable set GCP_DEPLOY_SA --body "$DEPLOY_SA_EMAIL" --repo "$GITHUB_REPO"
  gh variable set GCP_RUNTIME_SA --body "$RUNTIME_SA_EMAIL" --repo "$GITHUB_REPO"

Después del primer deploy del backend:

  gh variable set API_URL --body "https://<url-de-cloud-run>" --repo "$GITHUB_REPO"
  gh variable set CLOUDFLARE_PAGES_PROJECT --body "crud-initializr" --repo "$GITHUB_REPO"

Secretos de Cloudflare:

  gh secret set CLOUDFLARE_API_TOKEN --repo "$GITHUB_REPO"
  gh secret set CLOUDFLARE_ACCOUNT_ID --repo "$GITHUB_REPO"

Workload Identity Provider:

  $WIF_PROVIDER

Service accounts:

  deploy: $DEPLOY_SA_EMAIL
  runtime: $RUNTIME_SA_EMAIL
EOF
