# 🚀 Smart Attendance One-Click Starter (Windows)

Write-Host "--- Starting Smart Attendance System ---" -ForegroundColor Cyan

# 0. Stop existing processes to prevent Port Conflicts
Write-Host "[*] Cleaning up old processes..." -ForegroundColor Yellow
$processes = Get-CimInstance Win32_Process -Filter "name = 'java.exe' OR name = 'node.exe'"
foreach ($p in $processes) {
    if ($p.CommandLine -like "*smartAttendence*" -or $p.CommandLine -like "*next-server*" -or $p.CommandLine -like "*pnpm*") {
        Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
    }
}


# 1. Check if PostgreSQL is running
$pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue
if ($null -eq $pgService) {
    Write-Host "[!] Warning: PostgreSQL service not found. Make sure it is installed and running." -ForegroundColor Yellow
} elseif ($pgService.Status -ne "Running") {
    Write-Host "[*] Starting PostgreSQL service..." -ForegroundColor Green
    Start-Service -Name $pgService.Name -ErrorAction SilentlyContinue
} else {
    Write-Host "[OK] PostgreSQL is running." -ForegroundColor Green
}

# 2. Load Secure Environment Variables from .env
Write-Host "[*] Loading secure environment variables from .env..." -ForegroundColor Cyan
if (Test-Path "Backend\.env") {
    Get-Content "Backend\.env" | ForEach-Object {
        $line = $_.Trim()
        if ($line -and !$line.StartsWith("#") -and $line.Contains("=")) {
            $name, $value = $line -split '=', 2
            if ($name -and $value) {
                [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), "Process")
            }
        }
    }
    Write-Host "[OK] Environment loaded." -ForegroundColor Green
}

# 3. Start Backend in a new window
Write-Host "[*] Starting Backend (Production Mode)..." -ForegroundColor Green
$jarPath = Get-ChildItem -Path "Backend\target\*.jar" | Select-Object -First 1
if ($null -eq $jarPath) {
    Write-Error "Backend JAR not found! Please build the project first."
    exit
}
# Start-Process will inherit the 'Process' environment variables we just set
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd Backend; java -jar ""$($jarPath.FullName)"" --spring.profiles.active=local --server.port=10000"

# 4. Start Frontend in a new window
Write-Host "[*] Starting Frontend (Production Mode)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd frontend/web-dashboard; pnpm run start"

Write-Host "-------------------------------------------" -ForegroundColor Cyan
Write-Host "Success! Two new windows should have opened." -ForegroundColor Cyan
Write-Host "Backend: http://localhost:10000" -ForegroundColor White
Write-Host "Frontend: http://localhost:3000" -ForegroundColor White
Write-Host "-------------------------------------------" -ForegroundColor Cyan
Write-Host ""
Write-Host "🚀 NEXT STEPS FOR COLLEGE DEPLOYMENT:" -ForegroundColor Yellow
Write-Host "1. Start your Cloudflare Tunnel for the Backend (Port 10000)."
Write-Host "2. Copy the .trycloudflare.com URL."
Write-Host "3. Run 'update-client-url.ps1' and paste that URL."
Write-Host "4. Start a second Tunnel for the Frontend (Port 3000) for student access."
Write-Host "-------------------------------------------" -ForegroundColor Cyan

