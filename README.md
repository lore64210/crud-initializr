# crud-initializr

Creador de ABMs (CRUD) de Spring Boot. Desde una web en React se define el dominio (entidades, atributos, relaciones, composiciones y validaciones) y se descarga un zip con un proyecto Spring Boot 4.1.2 listo para usar.

## APIs

Cada proyecto se genera con una API REST (por defecto) o GraphQL, seleccionable desde la web:

- **REST**: un controller por entidad con endpoints bajo `/api/nombreEntidad`, un manejador global de errores y Swagger UI en `/swagger-ui.html` con el contrato OpenAPI en `/v3/api-docs`.
- **GraphQL**: un controller por entidad, un esquema en `src/main/resources/graphql/schema.graphqls` y GraphiQL habilitado en `/graphiql`. Las queries son `findAll<Entidad>` y `find<Entidad>ById`, y las mutations `create<Entidad>`, `update<Entidad>` y `delete<Entidad>`.

Los atributos simples soportados son `String`, `Integer`, `Long`, `Boolean`, `Date`, `DateTime`, `Time`, `Decimal`, `Double` y `UUID`. En GraphQL los tipos `Date`, `DateTime`, `Time`, `BigDecimal` y `UUID` se exponen como scalars propios, los ids como `ID`, `Long` como `Float` y las relaciones y composiciones con tipos de entrada anidados (`<Entidad>Input`).

## Bases de datos

Cada proyecto se genera con un único motor de base de datos, seleccionable desde la web:

- **PostgreSQL** (por defecto)
- **MariaDB**
- **MySQL**
- **SQLite** (archivo local, sin servidor)
- **H2** (embebida, ideal para desarrollo)
- **MongoDB** (NoSQL documental, con composiciones embebidas)

El proyecto generado incluye el driver o starter correspondiente, la configuración de conexión (con variables de entorno `DB_URL`, `DB_USER` y `DB_PASSWORD`, o `MONGODB_URI` para MongoDB), el `schema.sql` documentado para los dialectos SQL y, para los motores con servidor, un `docker-compose.yml` listo para levantar la base.

En SQL se soportan relaciones N:1 (`CUSTOM`), 1:N (`CUSTOM_LIST`), 1:1 (`CUSTOM_ONE_TO_ONE`, con foreign key única) y N:M (`CUSTOM_MANY_TO_MANY`, con tabla intermedia). En MongoDB se soportan composiciones 1:1 (`COMPOSITION`) y 1:N (`COMPOSITION_LIST`): la entidad destino se marca como documento embebido y no genera colección ni API propia.

Los tests de los proyectos SQL usan H2 en memoria, sin necesidad de Docker. Los tests de MongoDB usan Testcontainers y requieren Docker.

## Cache Redis

El cache es opcional por proyecto: se habilita desde el initializr y se elige qué entidades se cachean (por defecto, todas). El proyecto generado incluye `spring-boot-starter-data-redis`, un `CacheConfig` con TTL y prefijo de claves configurable, y anotaciones `@Cacheable`/`@CachePut`/`@CacheEvict` en los services de las entidades seleccionadas. Las escrituras actualizan o invalidan el cache y, si Redis no responde, la API sigue funcionando sin cache. Redis se agrega al `docker-compose.yml` del proyecto y los tests generados corren con cache deshabilitado.

## Proyectos guardados

Desde el header se puede abrir un drawer con los proyectos guardados en el `localStorage` del navegador. Cada proyecto conserva nombre, entidades, base de datos, tipo de API y configuración de Redis. Al abrir uno se puede guardar o descartar el trabajo actual; si el proyecto no tiene nombre, se pide uno antes de guardarlo. Los proyectos quedan solo en ese navegador: no se sincronizan entre dispositivos.

## Requisitos

- Java 17
- Maven 3.8+
- No hace falta instalar Node: el build de Maven descarga Node 20 y compila el frontend automáticamente.

## Build y ejecución

```bash
mvn clean install
java -jar target/crud-initializr-0.0.1-SNAPSHOT.jar
```

La aplicación queda disponible en [http://localhost:8080](http://localhost:8080).

Las plantillas del proyecto generado están en `src/main/resources/projectBase` y se leen desde el classpath, por lo que no hace falta copiar archivos a `/tmp` ni configurar rutas externas.

## Desarrollo del frontend

En la carpeta `frontend` los comandos npm funcionan normalmente:

```bash
cd frontend
npm install
npm start          # http://localhost:3000, con proxy a localhost:8080
npm run build      # genera el bundle en target/classes/static/built
npm run build:pages # genera frontend/dist para Cloudflare Pages
```

Para que el backend sirva el frontend compilado hay que ejecutar `mvn clean install` (o `npm run build`) antes de arrancar el jar.

## Tests

```bash
mvn test
```

Incluye tests del generador (dominio, servicios, tests, esquema SQL, relaciones 1:1 y N:M, composiciones MongoDB, APIs REST/GraphQL, Swagger/OpenAPI y pom) y de las utilidades.

## Deploy

El frontend se despliega en **Cloudflare Pages** y el backend en **Cloud Run**.

El detalle de variables, secretos y workflows está en [`docs/deploy.md`](docs/deploy.md).

- Frontend: `frontend`, build `npm run build:pages`, output `dist`.
- Backend: Docker → Artifact Registry → Cloud Run.
- GitHub Actions: `.github/workflows/deploy-backend.yml` y `.github/workflows/deploy-frontend.yml`.
- CORS: `CorsConfig` permite `*.pages.dev` y `localhost:3000`.
- No se usa volumen: la app es stateless.
