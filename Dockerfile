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

# =========================================================
# PERFORMANCE TUNING (Render Free Tier 512MB)
# - UseSerialGC: Minimal memory overhead
# - TieredStopAtLevel=1: Fast start, low CodeCache usage
# - SharedArchiveFile: CDS for ~50MB RAM savings
# - Xmx200m: Safe heap ceiling
# =========================================================

# Generate CDS Archive for fast, low-memory startup
RUN java -Dspring.context.exit=onRefresh -XX:ArchiveClassesAtExit=app.jsa -jar app.jar || true

USER appuser

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:SharedArchiveFile=app.jsa -XX:MaxRAMPercentage=45.0 -XX:MaxMetaspaceSize=128m -Xmx220m -Xss256k -jar app.jar"]


