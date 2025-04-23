# Dockerfile
FROM openjdk:17-jdk-slim
VOLUME /tmp
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]