# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copia tudo da pasta katsu_fit_backend
COPY katsu_fit_backend/ .

RUN gradle buildFatJar --no-daemon

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV PORT=8080
CMD ["java", "-jar", "app.jar"]