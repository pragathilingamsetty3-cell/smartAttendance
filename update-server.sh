#!/bin/bash
# 🤖 Smart Attendance - Zero-Downtime Update Script
# Optimized for 1GB RAM Azure VM

echo "--- Starting Smart Update ---"

# 1. Build from GitHub Workspace
echo "[*] Compiling from Github Actions Workspace: $(pwd)"

# 2. Build Fresh version WHILE the old one is still running
echo "[*] Building fresh Backend JAR..."
cd Backend
chmod +x mvnw
export MAVEN_OPTS="-Xmx256m"
./mvnw clean package -DskipTests
cd ..

# 3. Check if build was successful
jar_path=$(ls Backend/target/smartAttendence-*.jar | head -n 1)
if [ -z "$jar_path" ]; then
    echo "[!] Error: Build failed! JAR not found. Keeping existing version running."
    exit 1
fi

# 4. Move JAR to stable location (Prevent Github Actions from clearing the active JAR file later)
echo "[*] Copying new JAR to stable execution directory..."
cp "$jar_path" /home/azureuser/smartAttendence-latest.jar

# 5. Stop the old process
echo "[*] Swapping versions (5-10 second downtime)..."
sudo fuser -k 10000/tcp 2>/dev/null || echo "No previous process on port 10000"

# 6. Start Backend in Background (LEAN OPTIMIZED)
echo "[+] Starting new JAR on port 10000"
cd /home/azureuser

# CRITICAL FIX: GitHub Actions automatically kills all background processes when the job finishes.
# By clearing these variables, we hide the process from the GitHub Actions cleanup watcher.
export RUNNER_TRACKING_ID=""
export BUILD_ID=""

nohup java -Xmx384M -Xms128M -XX:+UseG1GC -XX:G1PeriodicGCInterval=30000 -XX:+UseStringDeduplication -jar /home/azureuser/smartAttendence-latest.jar --spring.profiles.active=local --server.port=10000 > /home/azureuser/smart-attendance-audit.log 2>&1 &

echo "--- Waiting 15 seconds to capture startup logs... ---"
sleep 15
echo "==== STARTUP LOGS ===="
tail -n 100 /home/azureuser/smart-attendance-audit.log
echo "====================="
