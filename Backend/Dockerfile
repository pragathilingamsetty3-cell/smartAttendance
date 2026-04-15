# 🏗️ Build Stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies (layer caching)
COPY pom.xml .
# Download dependencies without transitives in offline mode
RUN mvn dependency:go-offline -B --quiet || true

# Copy source code and build
COPY src ./src

# Production build with optimized compiler settings
# Xmx1024m: Heap for compilation
# XX:+TieredCompilation: Faster compilation with tiering
# XX:-TieredCompilation speeds up but uses more mem, so use tiered
# XX:TieredStopAtLevel=2: Reduce compilation overhead
RUN MAVEN_OPTS="-Xmx1024m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+TieredCompilation -XX:TieredStopAtLevel=1" \
    mvn clean package -B -DskipTests \
    -Dspring.profiles.active=prod \
    -Dspringdoc.skip=true \
    -q

# 🚀 Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the build artifact from build stage
COPY --from=build /app/target/smartAttendence-0.0.1-SNAPSHOT.jar app.jar

# 🛡️ Dynamic port exposure (Render will provide the PORT env var)
# No hardcoded EXPOSE needed, or use a comment

# 🧠 MEMORY OPTIMIZATION FOR 512MB RAM
# -XX:MaxRAMPercentage: Ensures JVM doesn't exceed 75% of container RAM (around 380MB)
# -Xss: Smaller thread stack size (256k) to save memory
ENTRYPOINT ["java", \
    "-XX:InitialRAMPercentage=40.0", \
    "-XX:MaxRAMPercentage=70.0", \
    "-XX:MinRAMPercentage=50.0", \
    "-Xss256k", \
    "-Dspring.profiles.active=prod", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
