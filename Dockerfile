# Build stage
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build

WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Pre-fetch dependencies
RUN ./mvnw dependency:go-offline -B

# Copy src and build
COPY src src
RUN ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=build /app/target/session-tracker-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
