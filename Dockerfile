FROM eclipse-temurin:17-jdk-alpine
EXPOSE 8080
ADD src/main/resources/static /tmp
VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/crud-initializr.jar"]