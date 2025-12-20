FROM maven:3.9.9-eclipse-temurin-17 AS builder
LABEL authors="shaan"
WORKDIR /app
COPY . .
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","app.jar"]