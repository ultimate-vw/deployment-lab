# ---- Stage 1: Build with Maven ----
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copy pom and source code
COPY pom.xml .
COPY src ./src

# Build the fat jar (skip tests for speed in CI)
RUN mvn clean package -DskipTests

# ---- Stage 2: Minimal runtime ----
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /workspace/target/*.jar app.jar

RUN mkdir -p /var/log/app

# Copy logback config if you have it (optional)
 COPY src/main/resources/logback-spring.xml ./

# If you want Spring Boot to use custom logback config
# ENV SPRING_LOGGING_CONFIG=file:./logback-spring.xml

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
