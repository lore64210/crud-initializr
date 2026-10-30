FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY target/*.jar app.jar
RUN chown -R app:app /app

USER app

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
