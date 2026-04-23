#!/bash

# ======================================================
# 🔍 SMART ATTENDANCE - AZURE ENVIRONMENT PRE-CHECK
# ======================================================

echo "--------------------------------------------------"
echo "🔍 Starting System Pre-Check..."
echo "--------------------------------------------------"

# 1. Check Java Version
if command -v java >/dev/null 2>&1; then
    JAVA_VER=$(java -version 2>&1 | head -n 1)
    echo "✅ Java Found: $JAVA_VER"
else
    echo "❌ Java NOT Found! Please install Java 21."
fi

# 2. Check Node.js Version
if command -v node >/dev/null 2>&1; then
    NODE_VER=$(node -v)
    echo "✅ Node.js Found: $NODE_VER"
else
    echo "❌ Node.js NOT Found! Please install Node.js."
fi

# 3. Check pnpm
if command -v pnpm >/dev/null 2>&1; then
    PNPM_VER=$(pnpm -v)
    echo "✅ pnpm Found: $PNPM_VER"
else
    echo "❌ pnpm NOT Found! Run: npm install -g pnpm"
fi

# 4. Check PostgreSQL
if command -v psql >/dev/null 2>&1; then
    echo "✅ PostgreSQL Client (psql) Found."
    # Check if DB exists
    export PGPASSWORD='Pragathi@2105'
    if psql -h localhost -U postgres -lqt | cut -d \| -f 1 | grep -qw smart_attendance; then
        echo "✅ Database 'smart_attendance' exists."
    else
        echo "⚠️ Database 'smart_attendance' NOT found! Create it using 'createdb smart_attendance'."
    fi
else
    echo "❌ PostgreSQL NOT Found! Please install PostgreSQL 16."
fi

# 5. Check .env File
if [ -f ".env" ]; then
    echo "✅ .env file found in root."
else
    echo "❌ .env file NOT found! System will use default (possibly incorrect) values."
fi

echo "--------------------------------------------------"
echo "🏁 Pre-Check Complete!"
echo "--------------------------------------------------"
