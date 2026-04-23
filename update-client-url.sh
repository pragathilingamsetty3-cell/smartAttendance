#!/bin/bash

# 🔗 Smart Attendance: Update Cloudflare URL (Linux)

echo "--------------------------------------------------"
echo "🔗 Cloudflare Tunnel URL Linker"
echo "--------------------------------------------------"

# Ask for the URL
read -p "Please enter the Cloudflare Tunnel URL (e.g., https://xyz.trycloudflare.com): " CLOUDFLARE_URL

if [ -z "$CLOUDFLARE_URL" ]; then
    echo "❌ ERROR: URL cannot be empty."
    exit 1
fi

# Path to frontend .env.production
ENV_FILE="frontend/web-dashboard/.env.production"

echo "[*] Updating environment configuration..."

# Create or overwrite the .env.production file
cat > "$ENV_FILE" << EOF
NEXT_PUBLIC_API_URL=$CLOUDFLARE_URL
NEXT_PUBLIC_ENVIRONMENT=production
EOF

echo "✅ Environment file updated: $ENV_FILE"

# Lock it in with a fresh build
echo "[*] Rebuilding frontend to lock in the new URL..."
cd frontend/web-dashboard
pnpm run build
cd ../..

echo "--------------------------------------------------"
echo "🎉 SUCCESS: Frontend is now linked to $CLOUDFLARE_URL"
echo "--------------------------------------------------"
