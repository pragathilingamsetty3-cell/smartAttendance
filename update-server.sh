#!/bin/bash
# 🤖 Smart Attendance - Zero-Downtime Update Script
# Optimized for 1GB RAM Azure VM

echo "--- Starting Smart Update ---"

# 1. Identity & Path Check
export HOME=/home/azureuser
cd /home/azureuser

# 2. Build Fresh version WHILE the old one is still running
# This reduces downtime from 5 minutes to 10 seconds.
echo "[*] Building fresh Backend JAR (Background process)..."
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

# 4. Now that we have the new JAR, stop the old process
echo "[*] Swapping versions (5-10 second downtime)..."
sudo fuser -k 10000/tcp 2>/dev/null || echo "No previous process on port 10000"

# 5. Start Backend in Background (LEAN OPTIMIZED)
echo "[+] Starting new JAR: $jar_path"
# -XX:G1PeriodicGCInterval=30000 returns unused RAM back to OS
nohup java -Xmx384M -Xms128M -XX:+UseG1GC -XX:G1PeriodicGCInterval=30000 -XX:+UseStringDeduplication -jar "$jar_path" --spring.profiles.active=local --server.port=10000 > /home/azureuser/smart-attendance-audit.log 2>&1 &

echo "--- Update Complete! System is initializing... ---"
echo "Check progress with: tail -f /home/azureuser/smart-attendance-audit.log"
