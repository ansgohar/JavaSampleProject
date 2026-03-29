# Stage 1: Build
FROM maven:3-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy only dependency-related files first for layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# Run as non-root user
RUN useradd --no-create-home --shell /bin/false appuser
USER appuser

COPY --from=builder /build/target/sonarqube-java-sample-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
