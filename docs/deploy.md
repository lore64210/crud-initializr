# Deploy: Cloudflare Pages + Cloud Run

Arquitectura elegida:

```text
GitHub
  ├── frontend → Cloudflare Pages (estático)
  └── backend  → Cloud Run (API + generación de ZIP)
```

No se usa volumen ni base de datos. El ZIP se genera en un directorio temporal y se borra después de la respuesta.

## Orden del primer deploy

1. Preparar GCP y desplegar el backend.
2. Obtener la URL de Cloud Run.
3. Configurar `API_URL` con esa URL.
4. Desplegar el frontend en Cloudflare Pages.

El frontend necesita la URL del backend **en tiempo de build**.

## Preparación de GCP

1. Crear el proyecto y una billing account.
2. Activar APIs:
   - `run.googleapis.com`
   - `artifactregistry.googleapis.com`
   - `iam.googleapis.com`
   - `iamcredentials.googleapis.com`
   - `cloudresourcemanager.googleapis.com`
3. Crear el repositorio Docker:

```bash
gcloud artifacts repositories create crud-initializr \
  --repository-format=docker \
  --location=us-central1
```

4. Crear `github-deployer` y `crud-run-sa`.
5. Darle a `github-deployer`:
   - `roles/run.admin`
   - `roles/artifactregistry.writer`
   - `roles/iam.serviceAccountUser` sobre `crud-run-sa`
6. Crear el Workload Identity Pool/Provider para GitHub, restringido a este repositorio.
7. Configurar un budget alert.

## GitHub Actions: backend

Workflow: `.github/workflows/deploy-backend.yml`

Variables del repositorio:

| Variable | Ejemplo |
|---|---|
| `GCP_PROJECT_ID` | `mi-proyecto` |
| `GCP_REGION` | `us-central1` |
| `GAR_REPOSITORY` | `crud-initializr` |
| `CLOUD_RUN_SERVICE` | `crud-initializr` |
| `GCP_WIF_PROVIDER` | `projects/.../providers/github` |
| `GCP_DEPLOY_SA` | `github-deployer@mi-proyecto.iam.gserviceaccount.com` |
| `GCP_RUNTIME_SA` | `crud-run-sa@mi-proyecto.iam.gserviceaccount.com` |

El workflow:

- Corre los tests con el frontend salteado.
- Construye la imagen Docker.
- La sube a Artifact Registry.
- Deploya en Cloud Run.
- Usa Workload Identity Federation, sin claves JSON.

Primero hay que desplegar el backend para obtener la URL de Cloud Run. Después se configura `API_URL` y se ejecuta el workflow del frontend.

## GitHub Actions: frontend

Workflow: `.github/workflows/deploy-frontend.yml`

Variables del repositorio:

| Variable | Ejemplo |
|---|---|
| `API_URL` | `https://crud-initializr-xxxxx-uc.a.run.app` |
| `CLOUDFLARE_PAGES_PROJECT` | `crud-initializr` |

Secretos del repositorio:

| Secreto | Uso |
|---|---|
| `CLOUDFLARE_API_TOKEN` | Token con permiso Cloudflare Pages:Edit |
| `CLOUDFLARE_ACCOUNT_ID` | ID de la cuenta de Cloudflare |

El workflow:

- Instala dependencias del frontend.
- Genera `frontend/dist` con `npm run build:pages`.
- Deploya con Wrangler a Cloudflare Pages.

La URL pública queda como `https://<proyecto>.pages.dev`.

Antes del primer deploy hay que crear el proyecto de Pages:

```bash
npx wrangler pages project create crud-initializr --production-branch=main
```

## Cloudflare Pages sin GitHub Actions

También se puede usar la integración Git nativa de Cloudflare Pages:

- Root directory: `frontend`
- Build command: `npm ci && npm run build:pages`
- Build output directory: `dist`
- Variable de entorno: `REACT_APP_API_URL=<URL de Cloud Run>`

En ese caso no hacen falta los secretos de Cloudflare en GitHub.

## CORS

El backend permite:

- `http://localhost:3000`
- `https://*.pages.dev`
- `https://*.vercel.app`

Configurado en `src/main/java/com/ahre/crudinitializr/config/CorsConfig.java`.

Si se agrega un dominio propio, hay que sumarlo a `allowedOriginPatterns`.

## Desarrollo local

Frontend servido por Spring Boot:

```bash
cd frontend
npm run build
cd ..
mvn spring-boot:run
```

Frontend estático separado:

```bash
cd frontend
REACT_APP_API_URL=http://localhost:8080 npm run build:pages
npx serve dist
```

El proxy de `package.json` solo se usa con `npm start`. El build estático apunta a `REACT_APP_API_URL`.

## Cloud Run

Configuración usada por el workflow:

- Región: `us-central1`
- `min-instances=0`
- `max-instances=3`
- 1 vCPU
- 1 GiB de RAM
- Concurrencia 4
- Timeout 300 s
- `--allow-unauthenticated`
- `--cpu-boost`

El contenedor escucha en `8080` y toma `PORT` si Cloud Run lo define.

## Costos

- Cloudflare Pages: estáticos gratis, sin costo por bandwidth en el plan Free.
- Cloud Run: free tier de requests/CPU/memoria.
- Egress de Cloud Run: solo API y ZIPs, porque el bundle del frontend ya no sale de Cloud Run.
- Los ZIPs pesan ~12 KB, así que el egress es mínimo.

Conviene configurar un budget alert en GCP.
