#!/bin/bash

# ======================================================
# 🚀 SMART ATTENDANCE - ULTRA-LIGHT AZURE DEPLOYMENT
# ======================================================

# 1. RAM Safety Net: Check/Create Swap (2GB)
if ! swapon --show | grep -q "/swapfile"; then
    echo "🧠 Creating 2GB Swap File (RAM Safety Net)..."
    sudo fallocate -l 2G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
    echo "✅ Swap File Created."
fi

# Run environment pre-check
if [ -f "./azure-pre-check.sh" ]; then
    chmod +x ./azure-pre-check.sh
    ./azure-pre-check.sh
fi

echo "[1/4] Setting Environment Variables..."
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=smart_attendance
export DB_USERNAME=postgres
export DB_PASSWORD='Pragathi@2105'
export JWT_ISSUER=Smart-Attendance-System
export JWT_PUBLIC_KEY='MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt1Xl3aci5Sxtiss0dV/0EWNTi6Dr03lpicpu5yLNlSBbqB8/fiepLQzcBqIPj1RRcK8GyNSQlaPV/UXZR732frtcS2PSYY+5lj92F0ziRuYzYkD40PIMWoIQNiH4xWkDBXiIcF6dzIPlbQoQhnXgeUrKg6JMQQ3tXfjU5Rc4hQBn189BXfsOVw5Bjbr/4dRZFt16wu77ZXX6LQWxvd1vGu0q1Kq1JC6XOJ8fUc9/KML63h8G3WKvvgtIta6pO0D1VMDeQqAFr2DPWchMrOSSqi1xXwNwY+sUjy5bUTQfFZjbkhLo34w7wIjGkM5kZbxr+5OZHlWpIxKd+Qnl25AoJQIDAQAB'
export JWT_PRIVATE_KEY='MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC3VeXdpyLlLG2KyzR1X/QRY1OLoOvTeWmJym7nIs2VIFuoHz9+J6ktDNwGog+PVFFwrwbI1JCVo9X9RdlHvfZ+u1xLY9Jhj7mWP3YXTOJG5jNiQPjQ8gxaghA2IfjFaQMFeIhwXp3Mg+VtChCGdeB5SsqDokxBDe1d+NTlFziFAGfXz0Fd+w5XDkGNuv/h1FkW3XrC7vtldfotBbG93W8a7SrUqrUkLpc4nx9Rz38owvreHwbdYq++C0i1rqk7QPVUwN5CoAWvYM9ZyEys5JKqLXFfA3Bj6xSPLltRNB8VmNuSEujfjDvAiMaQzmRlvGv7k5keVakjEp35CeXbkCglAgMBAAECggEAJYOp5NRfp1HTO2iPvCnNfSZcMNDVmKD6J8QYs+qS8x4bKzhzq0qNy0Zbmm7cbLl17a8e3gbi6//JmIkLqCq2juGjRdjv8l8wvE4RibyFe7j8kDEXyNeSkA2XqCAE8c4mXF742jd+JhxiHJEH5x+lNmSHsm5KGLCzHry9QYBiJBKjwCS/bDS0qDQt9pA7WSMnPlRxlT4YGz4B7dlFrLwJO7nsqk+SyKJl6peo5FZcyTmsWEHg31SjobFA3owwnYrFwbguCz2CgUBWPZrQ4HuaPQGEFeWAh44Z2Fjw5wUIEAgDv9GXDZCcKYKg5ERCG/tU1znQqvLSlGtopYkD3U/XuQKBgQDUtNtBMoE+LTpN0AO+HYtidhDIj1EjrWJvLQEPlhm0ol+bwX7D7MArK5r0stDlgmcSQ+PcMsSbwcdUqatWrEGHxodiCgnir+x9AAlB6KEAYinaZ/hgjk6xCDP8QxBJLvED9TRBes3adlqhA51oE/B/vSb1JW0HqqE8nv9HufhGvQKBgQDcpqizCvfVMf8rHIRoblZsjScM1jiC/xzDzzwfWfPdOBGyQMcPXDk90fNkPTiLj1F9LO67rIfxryKPJXuGB65rTGAPk8rG8j9JD0fE1Hi99i4v4GfdKf2McU9wySQRXWD9VTC5RKto8A+a0V9skqy1oaLhtDRV/AeVOsJ289rRiQKBgAj2CZqojwtYinFGxzGWOw4N3U0lxIxVPKVkZvwKHdEfWhnMwBNvAPWQo7mVYvmYUdFSLOJU/TV2p3goocBIB4a51XBK7fmv4a2ud7VgIJBMkmjSIoOm2yfYKaCCDsiWgq7hAK+VheCjRdQsu8/rryEijeacCgjdpmdQZ37VFeGdAoGARANXEsE5vUyI89f5dFs2ZoVn2QwbJT3Ptwek56EJi1HiGojfEvwBZO3XUTmRuWr22lfESrMWwEeUpn0OQzUQ7WAAaCzNH1/CZBnCrIg1o5BDklxgh4qO7gBrVT+az4NyBwSAXUsubs04cidBe60GcYIeO+YZD7v89mHDWXk3JMkCgYEAriVI9X425RabSRW07cfUTpXULEP9q6lKO+vxV8b1AnX9tVhJzj1VyyG+ymGlpVJsLwbt6/vrpE9guSedV7q7kPqtt88xmM0cGAdjofVnOMAoNxSIuNLLO/CLjUYmsIfyGic941qLsrbmADQ8yqYZAAqZflLDlf2hldshGN/rd1c='
export ALLOWED_ORIGINS='*'
export SPRING_PROFILES_ACTIVE=local
export EMAIL_USERNAME=pragathilingamsetty3@gmail.com
export EMAIL_PASSWORD=znvnakotauabooxt

# 2. Start Cloudflare (Arm64)
if ! command -v cloudflared &> /dev/null; then
    echo "📦 Installing Cloudflare Tunnel..."
    curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64.deb
    sudo dpkg -i cloudflared.deb
fi

# 3. Pull latest code from Git and always rebuild Backend
echo "[2/4] Starting Backend Engine (384MB Mode)..."
mkdir -p logs

echo "📦 Pulling latest code from Git..."
git pull origin main 2>&1 | tee -a logs/backend.log

echo "🔨 Building backend with latest changes..."
cd Backend && ./mvnw clean package -DskipTests 2>&1 | tail -n 5 && cd ..
JAR_FILE=$(ls Backend/target/*.jar 2>/dev/null | grep -v '\.original' | head -n 1)

nohup java -Xmx384m -jar "$JAR_FILE" > logs/backend.log 2>&1 &
BACKEND_PID=$!

# 4. Start Frontend (Production Mode)
echo "[3/4] Starting Frontend Dashboard (Production Mode)..."
cd frontend/web-dashboard
if [ ! -d "node_modules" ]; then pnpm install; fi
if [ ! -d ".next" ]; then
    echo "🏗️ Building Frontend (This may take 2-3 mins on 1GB RAM)..."
    pnpm run build
fi
export NEXT_PUBLIC_API_URL=http://4.188.248.38:10000
nohup pnpm run start --port 3000 --hostname 0.0.0.0 > ../../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
cd ../..

# 5. Start Cloudflare Tunnels
echo "[4/4] Generating Secure HTTPS Links..."
nohup cloudflared tunnel --url http://localhost:10000 > logs/tunnel-backend.log 2>&1 &
TUNNEL_BACKEND_PID=$!
nohup cloudflared tunnel --url http://localhost:3000 > logs/tunnel-frontend.log 2>&1 &
TUNNEL_FRONTEND_PID=$!

echo "--------------------------------------------------"
echo "🚀 SECURE DEPLOYMENT IS ACTIVE!"
echo "--------------------------------------------------"
echo "⏳ Waiting for tunnels... (run 'cat logs/tunnel-frontend.log | grep trycloudflare' in 10s)"
echo "💡 To stop: kill $BACKEND_PID $FRONTEND_PID $TUNNEL_BACKEND_PID $TUNNEL_FRONTEND_PID"
