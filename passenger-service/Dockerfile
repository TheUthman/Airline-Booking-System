FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY passenger-service /workspace/passenger-service
RUN --mount=type=cache,target=/root/.m2 mvn -q -f passenger-service/pom.xml -DskipTests package -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120
FROM eclipse-temurin:21-jre
COPY --from=build /workspace/passenger-service/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
