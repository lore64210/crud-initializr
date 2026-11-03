# Deploy: Cloud Run (frontend + backend)

Arquitectura:

```text
GitHub → GitHub Actions → Docker (frontend + backend) → Artifact Registry → Cloud Run
```

El build de Maven compila el frontend y lo deja dentro del jar. Cloud Run sirve la UI en `/` y la API en `/api/**` desde el mismo origen, así que no hace falta CORS ni un deploy separado.

No se usa volumen ni base de datos. El ZIP se genera en un directorio temporal y se borra después de la respuesta. La app es stateless, así que Cloud Run puede escalar a cero.

> Opcional: si querés publicar el frontend por separado en Cloudflare Pages, el workflow queda disponible para ejecución manual. No es necesario para el deploy normal.

## Antes de empezar

- Un proyecto de GCP con **billing habilitado**. Cloud Run tiene free tier, pero exige una billing account.
- Permisos para crear recursos en ese proyecto (Owner, o Project Creator + Service Usage Admin + Billing Account User).
- El repositorio en GitHub con la rama `main`.
- Una cuenta de Cloudflare con Pages habilitado.
- `gh` CLI autenticado en tu máquina (opcional, pero simplifica configurar variables y secretos).

Si el proyecto de GCP todavía no existe, crealo desde la consola y vinculale una billing account. El script de bootstrap no crea el proyecto: configura todo lo que va dentro.

## Paso 1: bootstrap de GCP

El script `scripts/bootstrap-gcp.sh` crea APIs, service accounts, Artifact Registry, IAM y Workload Identity Federation. Es idempotente: se puede volver a ejecutar.

La forma más simple es usar **Google Cloud Shell** (ya trae `gcloud`):

```bash
git clone https://github.com/lore64210/crud-initializr.git
cd crud-initializr

PROJECT_ID=tu-proyecto \
GITHUB_REPO=lore64210/crud-initializr \
bash scripts/bootstrap-gcp.sh
```

Variables opcionales:

```bash
REGION=us-central1
GAR_REPOSITORY=crud-initializr
SERVICE=crud-initializr
BRANCH=main
```

El script:

- Habilita `run`, `artifactregistry`, `iam`, `iamcredentials`, `cloudresourcemanager` y `serviceusage`.
- Crea `github-deployer` (deploy desde GitHub Actions) y `crud-run-sa` (runtime de Cloud Run).
- Crea el repositorio Docker `crud-initializr` en Artifact Registry.
- Aplica una política de retención de **2 versiones** para no salir del free tier de 0,5 GB.
- Da al deployer `run.admin`, `artifactregistry.writer` y `serviceusage.serviceConsumer`.
- Da a Cloud Run permiso de lectura sobre Artifact Registry.
- Crea el pool/provider de Workload Identity Federation restringido a tu repo y a la rama `main`.
- Imprime los comandos de `gh` para configurar GitHub.

En Windows, con `gcloud` instalado y autenticado, podés usar la versión PowerShell:

```powershell
.\scripts\bootstrap-gcp.ps1 `
  -ProjectId spring-crud-initializr `
  -GarRepository spring-crud-initializr `
  -Service spring-crud-initializr `
  -GitHubRepo lore64210/crud-initializr
```

Al finalizar, copiá los comandos que imprime el script.

## Paso 2: configurar GitHub

Si usás `gh`:

```bash
gh auth login
```

Después ejecutá los comandos que imprimió el bootstrap. Equivalentemente, desde Settings → Secrets and variables → Actions → Variables:

| Variable | Ejemplo | Uso |
|---|---|---|
| `GCP_PROJECT_ID` | `mi-proyecto` | Proyecto de GCP |
| `GCP_REGION` | `us-central1` | Región de Cloud Run y Artifact Registry |
| `GAR_REPOSITORY` | `crud-initializr` | Repositorio Docker |
| `CLOUD_RUN_SERVICE` | `crud-initializr` | Nombre del servicio |
| `GCP_WIF_PROVIDER` | `projects/123.../locations/global/workloadIdentityPools/github-pool/providers/github` | Workload Identity |
| `GCP_DEPLOY_SA` | `github-deployer@mi-proyecto.iam.gserviceaccount.com` | Deployer |
| `GCP_RUNTIME_SA` | `crud-run-sa@mi-proyecto.iam.gserviceaccount.com` | Runtime de Cloud Run |

El workflow usa Workload Identity Federation: **no hay claves JSON** en GitHub.

La condición del provider permite solo `refs/heads/main` de tu repositorio. Si lanzás el workflow manualmente, hacelo desde `main`.

### Rama por defecto

GitHub muestra los workflows de la rama por defecto. Si el repo quedó con `master` como default y los workflows viven en `main`, cambiala:

```bash
gh repo edit lore64210/crud-initializr --default-branch main
```

## Paso 3: deploy

Con las variables configuradas, hacé push a `main` o lanzá el workflow a mano:

```bash
gh workflow run deploy-backend.yml --ref main
gh run watch
```

El workflow:

1. Valida que estén todas las variables.
2. Corre `mvn verify`, que compila el frontend y lo embebe en el jar.
3. Se autentica en GCP con WIF.
4. Construye la imagen Docker y la sube a Artifact Registry con tags `latest` y el SHA del commit.
5. Deploya en Cloud Run con `min-instances=0`, `max-instances=3`, 1 vCPU, 1 GiB, concurrencia 4 y timeout 300 s.
6. Hace un smoke test contra `/`, `/built/bundle.js` y `/built/css/app.css`.

Al terminar, obtené la URL:

```bash
gcloud run services describe crud-initializr \
  --region us-central1 \
  --format='value(status.url)'
```

También queda en el summary del workflow.

## Paso 4: verificar

Abrí la URL de Cloud Run: debería cargar la UI. El mismo origen sirve la API, así que no hay CORS de por medio.

```bash
curl -I https://<url-de-cloud-run>/
curl -I https://<url-de-cloud-run>/built/bundle.js
```

## Opción B: frontend separado en Cloudflare Pages

No es necesario para el deploy normal. Solo si querés publicar el frontend por separado, por ejemplo para descargar el tráfico estático de Cloud Run.

El frontend necesita la URL del backend **en tiempo de build**.

### Crear el proyecto de Cloudflare Pages

Desde `frontend`:

```bash
cd frontend
npx wrangler pages project create crud-initializr --production-branch=main
```

También se puede crear desde el dashboard de Cloudflare.

### Token de Cloudflare

Creá un API Token con permiso **Account → Cloudflare Pages → Edit**. El Account ID está en el dashboard de Cloudflare.

### Variables y secretos en GitHub

```bash
gh variable set API_URL --body "https://<url-de-cloud-run>" --repo lore64210/crud-initializr
gh variable set CLOUDFLARE_PAGES_PROJECT --body "crud-initializr" --repo lore64210/crud-initializr

gh secret set CLOUDFLARE_API_TOKEN --repo lore64210/crud-initializr
gh secret set CLOUDFLARE_ACCOUNT_ID --repo lore64210/crud-initializr
```

### Ejecutar el deploy

```bash
gh workflow run deploy-frontend.yml --ref main
gh run watch
```

La URL queda como `https://crud-initializr.pages.dev`.

Si cambiás `API_URL`, volvé a ejecutar el workflow del frontend: la URL se inyecta en el bundle durante el build.

## Flujo automático

- `deploy-backend.yml` corre con pushes a `main` que toquen `src/**`, `frontend/**`, `pom.xml`, `Dockerfile`, `.dockerignore` o el propio workflow. Compila frontend y backend en la misma imagen.
- `deploy-frontend.yml` es opcional y solo corre manualmente (`workflow_dispatch`).

## Costos y límites

- **Cloud Run free tier**: 2M requests, 180.000 vCPU-seconds y 360.000 GiB-seconds por mes, más 1 GB de egress desde Norteamérica. Con `min-instances=0` y tráfico bajo, el costo esperado es USD 0.
- **Artifact Registry free tier**: 0,5 GB. La política de retención deja solo 2 versiones. Si la imagen crece y supera ese espacio, el cargo es mínimo (~USD 0,10/GB-mes); en ese caso bajá `keepCount` a 1.
- **Cloudflare Pages** (opcional): estáticos gratis, sin costo por bandwidth en el plan Free.
- **Egress de Cloud Run**: UI, API y ZIPs (~12 KB cada uno). El bundle del frontend se sirve desde Cloud Run y cuenta contra el 1 GB gratis desde Norteamérica. Si el tráfico crece, la opción B (Cloudflare Pages) descarga ese tráfico.

Configurá un budget alert:

```bash
gcloud billing accounts list
gcloud billing budgets create \
  --billing-account=<BILLING_ACCOUNT_ID> \
  --display-name="crud-initializr" \
  --budget-amount=5USD \
  --threshold-rule=percent=0.5 \
  --threshold-rule=percent=0.9 \
  --threshold-rule=percent=1.0
```

## Rollback

Las revisiones de Cloud Run son inmutables. Para volver a una anterior:

```bash
gcloud run revisions list --service crud-initializr --region us-central1
gcloud run services update-traffic crud-initializr \
  --region us-central1 \
  --to-revisions=<REVISION>=100
```

## Troubleshooting

**El workflow no aparece en Actions**

La rama por defecto debe ser `main` y el workflow tiene que existir en esa rama. Revisá con `gh repo view --json defaultBranchRef`.

**`403` al autenticar con Workload Identity**

La condición del provider debe coincidir con `owner/repo` y `refs/heads/main`. Verificá:

```bash
gcloud iam workload-identity-pools providers describe github \
  --location=global \
  --workload-identity-pool=github-pool
```

**`iam.serviceaccounts.actAs` denegado**

Falta `roles/iam.serviceAccountUser` del deployer sobre `crud-run-sa`. El bootstrap lo asigna; si cambiaste el nombre del runtime SA, volvé a ejecutarlo.

**Cloud Run no puede bajar la imagen**

Asigná `roles/artifactregistry.reader` al service agent de Cloud Run:

```bash
gcloud projects add-iam-policy-binding <PROYECTO> \
  --member="serviceAccount:service-<PROJECT_NUMBER>@serverless-robot-prod.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.reader"
```

**El smoke test falla**

Mirá los logs:

```bash
gcloud run services logs read crud-initializr --region us-central1 --limit 100
```

**CORS en el frontend separado**

Con el deploy single-service no hay CORS. Solo aplica a la opción B: `CorsConfig` permite `http://localhost:3000`, `https://*.pages.dev` y `https://*.vercel.app`. Si usás un dominio propio, agregalo a `allowedOriginPatterns` y volvé a desplegar el backend.

**Cold start**

Con `min-instances=0`, la primera request después de un rato puede tardar unos segundos (JVM). Si necesitás latencia constante, poné `--min-instances=1`, pero deja de ser gratis en todos los casos.

## Comandos útiles

```bash
# URL del servicio
gcloud run services describe crud-initializr --region us-central1 --format='value(status.url)'

# Logs
gcloud run services logs read crud-initializr --region us-central1 --limit 100

# Revisiones
gcloud run revisions list --service crud-initializr --region us-central1

# Imágenes en Artifact Registry
gcloud artifacts docker images list us-central1-docker.pkg.dev/<PROYECTO>/crud-initializr

# Variables y secretos de GitHub
gh variable list --repo lore64210/crud-initializr
gh secret list --repo lore64210/crud-initializr
```
