#!/bin/bash

# 🏁 Smart Attendance: Linux Production Startup Script

echo "🚀 Starting Smart Attendance System in PRODUCTION MODE..."

# 1. Ensure logs directory exists
mkdir -p logs

# 2. Start Backend (Spring Boot JAR)
echo "[*] Starting Backend API on Port 10000..."
JAR_FILE=$(ls Backend/target/*.jar | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "❌ ERROR: Backend JAR file not found! Please build the backend first."
    exit 1
fi

# Run in background with logging
nohup java -Dserver.port=10000 -jar "$JAR_FILE" --spring.profiles.active=local > logs/backend.log 2>&1 &
BACKEND_PID=$!
echo "✅ Backend started with PID: $BACKEND_PID"

# 3. Start Frontend (Next.js)
echo "[*] Starting Frontend Dashboard on Port 3000..."
cd frontend/web-dashboard

# Check if node_modules exists, if not, try to install
if [ ! -d "node_modules" ]; then
    echo "📦 Installing frontend dependencies..."
    npm install -g pnpm
    pnpm install
fi

# Start Next.js in production mode
nohup pnpm run start > ../../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
cd ../..
echo "✅ Frontend started with PID: $FRONTEND_PID"

echo "--------------------------------------------------"
echo "🌍 System is loading!"
echo "📍 API: http://localhost:10000"
echo "📍 Dashboard: http://localhost:3000"
echo "📷 View logs with: tail -f logs/backend.log"
echo "--------------------------------------------------"
echo "💡 To stop the system, run: kill $BACKEND_PID $FRONTEND_PID"
