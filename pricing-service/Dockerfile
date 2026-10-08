FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pricing-service /workspace/pricing-service
RUN --mount=type=cache,target=/root/.m2 mvn -q -f pricing-service/pom.xml -Dmaven.test.skip=true package
FROM eclipse-temurin:21-jre
COPY --from=build /workspace/pricing-service/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
