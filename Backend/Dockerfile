# 🏗️ Build Stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
# Skip tests for faster deployment and reduced memory footprint during build
RUN mvn package -DskipTests -B

# 🚀 Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the build artifact from build stage
COPY --from=build /app/target/smartAttendence-0.0.1-SNAPSHOT.jar app.jar

# 🛡️ EXPOSE the standard port (Render will proxy this to 443)
EXPOSE 8443

# 🧠 MEMORY OPTIMIZATION FOR 512MB RAM
# -XX:MaxRAMPercentage: Ensures JVM doesn't exceed 75% of container RAM (around 380MB)
# -Xss: Smaller thread stack size (256k) to save memory
ENTRYPOINT ["java", \
    "-XX:InitialRAMPercentage=40.0", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:MinRAMPercentage=50.0", \
    "-Xss256k", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
