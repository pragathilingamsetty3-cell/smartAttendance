#!/bin/bash
# 🤖 Smart Attendance - Native Linux Update Script
# Optimized for 1GB RAM Azure VM

echo "--- Starting Native Linux Update ---"

# 1. Identity & Path Check
export HOME=/home/azureuser
cd /home/azureuser

# 2. Stop existing processes to free up RAM
echo "[*] Freeing up memory..."
fuser -k 10000/tcp 2>/dev/null

# 3. Memory-Protected Build (Safe for 1GB RAM)
echo "[*] Building fresh Backend JAR with RAM Shield (256MB Maven Limit)..."
cd Backend
chmod +x mvnw
export MAVEN_OPTS="-Xmx256m"
./mvnw clean install -DskipTests
cd ..

# 4. Start Backend in Background (LEAN OPTIMIZED)
echo "[*] Restarting Backend (Lean Mode: 384MB Cap + Aggressive RAM Return)..."
jar_path=$(ls Backend/target/smartAttendence-*.jar | head -n 1)

if [ -z "$jar_path" ]; then
    echo "[!] Error: Backend JAR not found!"
else
    echo "[+] Starting JAR: $jar_path"
    # -XX:G1PeriodicGCInterval=30000 tells Java to return unused RAM to the OS every 30 seconds
    nohup java -Xmx384M -Xms128M -XX:+UseG1GC -XX:G1PeriodicGCInterval=30000 -XX:+UseStringDeduplication -jar "$jar_path" --spring.profiles.active=local --server.port=10000 > /home/azureuser/smart-attendance-audit.log 2>&1 &
fi

echo "--- Update Complete! System is running in background. ---"
