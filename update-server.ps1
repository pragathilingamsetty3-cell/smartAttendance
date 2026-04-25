# 🤖 Smart Attendance - Automated Update Script
# This script is triggered by GitHub Actions when you push code from your laptop.

Write-Host "--- Starting Automated Update Process ---" -ForegroundColor Cyan

# 1. Stop existing processes
Write-Host "[*] Stopping existing Backend and Frontend..." -ForegroundColor Yellow
$processes = Get-CimInstance Win32_Process -Filter "name = 'java.exe' OR name = 'node.exe'"
foreach ($p in $processes) {
    if ($p.CommandLine -like "*smartAttendence*" -or $p.CommandLine -like "*next-server*" -or $p.CommandLine -like "*pnpm*") {
        Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
    }
}


# 2. Cleanup & Optimization
Write-Host "[*] Building fresh Backend JAR (this may take 1-2 minutes)..." -ForegroundColor Yellow
cd Backend
./mvnw.cmd clean install -DskipTests
cd ..

# 3. Start Backend in Background
Write-Host "[*] Restarting Backend (ELITE Local Profile)..." -ForegroundColor Green
$jarPath = Get-ChildItem -Path "Backend\target\smartAttendence-*.jar" | Select-Object -First 1
if ($null -eq $jarPath) {
    Write-Host "[!] Error: Backend JAR not found! Checking build logs..." -ForegroundColor Red
} else {
    Write-Host "[+] Found JAR: $($jarPath.Name). Starting now..." -ForegroundColor Cyan
    Start-Process powershell -WindowStyle Hidden -ArgumentList "-Command", "cd Backend; java -jar ""$($jarPath.FullName)"" --spring.profiles.active=local --server.port=10000"
}

# 4. Start Frontend in Background
Write-Host "[*] Restarting Frontend (Production Mode)..." -ForegroundColor Green
Start-Process powershell -WindowStyle Hidden -ArgumentList "-Command", "cd frontend/web-dashboard; pnpm run start"


Write-Host "--- Update Complete! System is restarting in the background. ---" -ForegroundColor Cyan
Write-Host "You can check the audit log for any errors: smart-attendance-audit.log"
