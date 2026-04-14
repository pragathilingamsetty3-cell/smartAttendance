# 🏗️ Build Stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven Wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN ./mvnw package -DskipTests -B

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
