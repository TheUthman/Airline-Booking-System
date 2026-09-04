FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY api-gateway /workspace/api-gateway
RUN --mount=type=cache,target=/root/.m2 mvn -q -f api-gateway/pom.xml -DskipTests package -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120
FROM eclipse-temurin:21-jre
COPY --from=build /workspace/api-gateway/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
