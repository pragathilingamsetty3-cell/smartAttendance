# 🏗️ Build Stage - Minimal and Fast for 512MB Render Free Tier
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml only and download dependencies
COPY pom.xml .
# Aggressive dependency resolution with parallel downloads
RUN mvn dependency:resolve -B -q --fail-never && \
    mvn dependency:resolve-plugins -B -q --fail-never || true

# Copy source code and build
COPY src ./src

# Aggressive build: reduced memory, skip optional processing, single-threaded
RUN MAVEN_OPTS="-Xmx512m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:+DisableExplicitGC" \
    mvn package -B \
    -DskipTests \
    -Dmaven.test.skip=true \
    -DskipITs \
    -Dmaven.javadoc.skip=true \
    -Denforcer.skip=true \
    -Dspringdoc.skip=true \
    -q \
    -T 1C

# 🚀 Runtime Stage - Java Runtime with Native Libraries (Ubuntu-based for Firebase/gRPC support)
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy JAR from build stage and rename to app.jar
COPY --from=build /app/target/smartAttendence-*.jar ./app.jar

# Security: run as non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser && \
    chown -R appuser:appgroup /app

# Firebase service account is mounted via Render/Cloud Run secrets at runtime
ENV FIREBASE_SERVICE_ACCOUNT_PATH=/etc/secrets/serviceAccountKey.json
ENV SPRING_PROFILES_ACTIVE=prod

# Expose default port (Cloud Run uses $PORT, but 8080 is standard)
EXPOSE 8080

USER appuser

# 🧠 MEMORY OPTIMIZATION FOR CLOUD RUN
# -XX:MaxRAMPercentage: Ensures JVM doesn't exceed 75% of container RAM
# -Xss: Smaller thread stack size (256k) to save memory
ENTRYPOINT ["sh", "-c", "java -XX:InitialRAMPercentage=40.0 -XX:MaxRAMPercentage=75.0 -XX:MinRAMPercentage=50.0 -Xss256k -Dspring.profiles.active=prod -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -jar app.jar"]
