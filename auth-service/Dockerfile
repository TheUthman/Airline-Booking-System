FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY auth-service/pom.xml /workspace/auth-service/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -f auth-service/pom.xml \
    dependency:go-offline \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120

COPY auth-service/src /workspace/auth-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -f auth-service/pom.xml \
    clean package \
    -DskipTests \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/authservice/target/auth-service-0.0.1-SNAPSHOT.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]