# syntax=docker/dockerfile:1

# ---------- Stage 1: build the JAR with Maven + full JDK ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only the pom first and download dependencies into their own layer.
# This layer is reused from cache until pom.xml changes, so a source-only
# change does not re-download the whole dependency tree.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy the sources and build (tests already ran in the Jenkins Test stage).
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: slim runtime with only a JRE ----------
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="calculator-api" \
      org.opencontainers.image.description="Calculator REST API (Spring Boot) - CSE 816 SPE mini-project" \
      org.opencontainers.image.source="https://github.com/NirbhaySharma504/calculator-devops"

# Run as an unprivileged user instead of root.
RUN addgroup -S spring && adduser -S -G spring spring
WORKDIR /app

# Only the JAR is copied from the build stage: no Maven, no JDK, no sources.
COPY --from=build --chown=spring:spring /workspace/target/calculator.jar app.jar

USER spring
EXPOSE 8080

# JVM flags can be tuned at runtime (docker run -e / compose environment);
# the JVM reads JAVA_TOOL_OPTIONS automatically.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
