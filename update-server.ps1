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


# 2. Memory-Protected Build (Safe for 1GB RAM)
Write-Host "[*] Building fresh Backend JAR with RAM Shield (256MB Maven Limit)..." -ForegroundColor Yellow
cd Backend
$env:MAVEN_OPTS="-Xmx256m"
./mvnw.cmd clean install -DskipTests
cd ..

# 3. Start Backend in Background (LEAN OPTIMIZED)
Write-Host "[*] Restarting Backend (Lean Mode: 384MB Cap + Aggressive RAM Return)..." -ForegroundColor Green
$jarPath = Get-ChildItem -Path "Backend\target\smartAttendence-*.jar" | Select-Object -First 1
if ($null -eq $jarPath) {
    Write-Host "[!] Error: Backend JAR not found! Checking build logs..." -ForegroundColor Red
} else {
    Write-Host "[+] Found JAR: $($jarPath.Name). Starting with Lean Memory Tuning..." -ForegroundColor Cyan
    # -XX:G1PeriodicGCInterval=30000 tells Java to return unused RAM to the OS every 30 seconds
    Start-Process powershell -WindowStyle Hidden -ArgumentList "-Command", "cd Backend; java -Xmx384M -Xms128M -XX:+UseG1GC -XX:G1PeriodicGCInterval=30000 -XX:+UseStringDeduplication -jar ""$($jarPath.FullName)"" --spring.profiles.active=local --server.port=10000"
}

# 4. Start Frontend in Background (DISABLED - Moved to Cloudflare Pages to save 150MB RAM)
Write-Host "[*] Frontend is being handled by Cloudflare Pages. Skipping local start to save RAM." -ForegroundColor Yellow
# Start-Process powershell -WindowStyle Hidden -ArgumentList "-Command", "cd frontend/web-dashboard; pnpm run start"


Write-Host "--- Update Complete! System is restarting in the background. ---" -ForegroundColor Cyan
Write-Host "[DIAGNOSTIC] Current Real-Time RAM Usage on Azure:" -ForegroundColor Yellow
Get-Process | Where-Object {$_.Name -match "java|node"} | Select-Object Name, @{Name="Actual_RAM_MB";Expression={[Math]::Round($_.WorkingSet64 / 1MB, 2)}} | Format-Table -AutoSize
Write-Host "You can check the audit log for any errors: smart-attendance-audit.log"
