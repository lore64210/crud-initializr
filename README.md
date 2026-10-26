# crud-initializr

Creador de ABMs (CRUD) de Spring Boot. Desde una web en React se define el dominio (entidades, atributos, relaciones y validaciones) y se descarga un zip con un proyecto Spring Boot 4.1.2 listo para usar.

## Bases de datos

Cada proyecto se genera con un único motor de base de datos, seleccionable desde la web:

- **PostgreSQL** (por defecto)
- **MariaDB**
- **MySQL**
- **SQLite** (archivo local, sin servidor)
- **H2** (embebida, ideal para desarrollo)

El proyecto generado incluye el driver correspondiente, la configuración de conexión (con variables de entorno `DB_URL`, `DB_USER` y `DB_PASSWORD`), el `schema.sql` documentado para el dialecto elegido y, para los motores con servidor, un `docker-compose.yml` listo para levantar la base.

Los tests del proyecto generado siempre usan H2 en memoria, sin necesidad de Docker.

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
npm start      # http://localhost:3000, con proxy a localhost:8080
npm run build  # genera el bundle en target/classes/static/built
```

Para que el backend sirva el frontend compilado hay que ejecutar `mvn clean install` (o `npm run build`) antes de arrancar el jar.

## Tests

```bash
mvn test
```

Incluye tests del generador (generación de dominio, servicios, tests, esquema SQL y pom) y de las utilidades.

## Deploy

Para el despliegue subí la imagen de Docker a Docker Hub y desplegué en GCP:

```bash
docker build -t spring-crud-initializr .
docker tag spring-crud-initializr lore64210/spring-crud-initializr
docker push lore64210/spring-crud-initializr
```

Desplegado en Google Cloud Run. No se necesita subir la imagen al Artifact Registry: se puede usar Docker Hub directamente.
