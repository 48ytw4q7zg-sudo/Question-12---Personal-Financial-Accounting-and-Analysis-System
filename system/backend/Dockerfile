# Q-CR v13 Deployment — creator qxw · 2501060122
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
