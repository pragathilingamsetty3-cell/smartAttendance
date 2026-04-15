# =========================================================
# Stage 1: Build — Maven + JDK 21
# =========================================================
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Copy Maven wrapper and POMs first (layer cache optimisation)
COPY Backend/mvnw ./mvnw
COPY Backend/.mvn ./.mvn
COPY Backend/pom.xml ./pom.xml

# Pre-download dependencies (cached unless pom.xml changes)
RUN chmod +x ./mvnw && \
    ./mvnw dependency:go-offline -B --no-transfer-progress -q

# Copy source and build (skip tests for faster image build)
COPY Backend/src ./src
RUN ./mvnw package -DskipTests -B --no-transfer-progress -q

# =========================================================
# Stage 2: Runtime — minimal JRE 21
# =========================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the fat JAR from build stage
COPY --from=build /workspace/target/*.jar app.jar

# Firebase service account is mounted at runtime via Cloud Run secret
ENV FIREBASE_SERVICE_ACCOUNT_PATH=/secrets/firebase/serviceAccountKey.json
ENV SPRING_PROFILES_ACTIVE=prod

# Cloud Run expects the app to listen on $PORT (default 8080)
EXPOSE 8080

USER appuser

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dserver.port=${PORT:8080}", \
  "-jar", "app.jar"]
