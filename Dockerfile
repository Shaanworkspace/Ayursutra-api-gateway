
FROM openjdk:21-jdk-slim
WORKDIR /app
# JAR copy karein (Actions build karega)
COPY target/*.jar app.jar
# 1GB RAM ke liye memory limits zaruri hain
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
EXPOSE 8085