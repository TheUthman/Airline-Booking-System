FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY config-server /workspace/config-server
RUN --mount=type=cache,target=/root/.m2 mvn -q -f config-server/pom.xml -DskipTests package -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/config-server/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
