FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
