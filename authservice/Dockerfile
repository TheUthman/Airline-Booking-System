FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY authservice /workspace/authservice
RUN --mount=type=cache,target=/root/.m2 mvn -q -f authservice/pom.xml -DskipTests package -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120
FROM eclipse-temurin:21-jre
COPY --from=build /workspace/authservice/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
