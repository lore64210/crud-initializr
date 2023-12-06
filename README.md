# crud-initializr
Creador de abms spring-boot

En carpeta frontend, los comandos npm funcionan normalmente
npm start -> localhost:3000

Para que el frontend pueda consumir al backend, se tiene que correr mvn clean install para generar el build
Al ejecutar el jar, o la aplicacion spring, se puede ver la web en localhost:8000

Para el despliegue, subi la imagen de docker a dockerhub y desplegue en gcp

docker build -t spring-crud-initializr .  

docker tag spring-crud-initializr lore64210/spring-crud-initializr

docker push lore64210/spring-crud-initializr


Desplegado en google cloud run. No se necesitaa subir la imagen al artifact registry, se puede usar dockerhub directamente.
