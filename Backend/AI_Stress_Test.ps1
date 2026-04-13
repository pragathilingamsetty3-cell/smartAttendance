# 🚀 AI 'COMMANDER' SIMULATION SUITE (V12 - PRECISION VERIFICATION)

param (
    [int]$TotalStudents = 10,
    [int]$PresentTarget = 5,
    [int]$AbsentTarget = 2,
    [int]$WalkOutTarget = 2,
    [int]$AnomalyTarget = 1
)

$ErrorActionPreference = "Continue"
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$baseUrl = "https://localhost:8443/api/v1"
$adminEmail = "super.admin@smartattendence.com"
$adminPassword = "Pragathi@2105"
$studentPassword = "User_Smart@2026"

Write-Host "--- STARTING PRECISION AI VERIFICATION V12 ---" -ForegroundColor Cyan
Write-Host "TARGETS PER SECTION: P:$PresentTarget | A:$AbsentTarget | W:$WalkOutTarget | Anomaly:$AnomalyTarget" -ForegroundColor Gray

# 1. Login as Admin
Write-Host "Authenticating Admin..." -ForegroundColor Gray
try {
    $auth = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body (@{ email = $adminEmail; password = $adminPassword } | ConvertTo-Json)
    $headers = @{ Authorization = "Bearer $($auth.accessToken)" }
} catch {
    Write-Host "❌ Admin Authentication Failed!" -ForegroundColor Red
    exit
}

# 2. Discover active sessions
$stats = Invoke-RestMethod -Uri "$baseUrl/ai-analytics/dashboard" -Method Get -Headers $headers
$activeSessions = $stats.activeSessions

if ($null -eq $activeSessions -or $activeSessions.Count -eq 0) {
    Write-Host "⚠️ No active sessions found!" -ForegroundColor Yellow
    exit
}

Write-Host "Found $($activeSessions.Count) active sections. Deploying verification scenarios..." -ForegroundColor Green

foreach ($sessionSummary in $activeSessions) {
    $sessId = $sessionSummary.id
    $sectionName = $sessionSummary.sectionName
    $roomLat = $sessionSummary.latitude
    $roomLng = $sessionSummary.longitude
    
    # 🛡️ LEGACY GUARD: Skip 'CS-A' and other old naming artifacts to avoid credential errors
    if ($sectionName -like "CS-*") {
        Write-Host "⚠️ Skipping Legacy Section: $sectionName (Naming mismatch with new Seeder)" -ForegroundColor DarkYellow
        continue
    }

    # Parse Dept and Section Suffix (e.g., "CSE-A" -> "cse" and "a")
    $parts = $sectionName -split "-"
    if ($parts.Count -lt 2) { continue }
    
    $deptCode = $parts[0].ToLower()
    $sectionSuffix = $parts[1].ToLower()
    
    Write-Host "Testing Section: $sectionName" -ForegroundColor Yellow

    $count_P = 0; $count_A = 0; $count_W = 0; $count_Anom = 0; $count_Fail = 0

    for ($s = 1; $s -le 10; $s++) {
        $email = "student.$deptCode.$sectionSuffix.$s@smart.local"
        
        # Determine Behavior (Strict 5-2-2-1 Ratio for Exactly 10 Students)
        $behavior = "ABSENT"
        if ($s -le 5) { $behavior = "PRESENT" }
        elseif ($s -le 7) { $behavior = "ABSENT" }
        elseif ($s -le 9) { $behavior = "WALK_OUT" }
        elseif ($s -eq 10) { $behavior = "ANOMALY" }

        if ($behavior -eq "ABSENT") {
            Write-Host "   [SKIP] $email -> $behavior" -ForegroundColor DarkGray
            $count_A++
            continue
        }

        try {
            $login = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body (@{ email = $email; password = $studentPassword } | ConvertTo-Json)
            
            $lat = $roomLat; $lng = $roomLng
            $status = "STILL"; $steps = 0; $acc = 0.1
            $fingerprint = "SIM-DEVICE-$email"
            $wifi = "TechUniversity-Secure"

            if ($behavior -eq "PRESENT") {
                $lat += (Get-Random -Minimum -0.00003 -Maximum 0.00003)
                $lng += (Get-Random -Minimum -0.00003 -Maximum 0.00003)
                $count_P++
            }
            elseif ($behavior -eq "WALK_OUT") {
                $lat += 0.0008; $lng += 0.0008 # Outside
                $status = "WALKING"; $steps = 200; $acc = 2.0
                $count_W++
            }
            elseif ($behavior -eq "ANOMALY") {
                # 🚨 LOCATION SPOOFING: Extreme motion anomaly (Hacking simulation)
                $lat += 0.00001; $lng += 0.00001
                $acc = 25.0 # Exceeds 20g threshold for Spoofing detection
                # Counter moved to catch block for confirmation
            }

            $pulse = @{ 
                studentId = $login.user.id; sessionId = $sessId; 
                latitude = $lat; longitude = $lng; gpsAccuracy = (Get-Random -Minimum 2.0 -Maximum 30.0);
                accelerationX = $acc; accelerationY = $acc; accelerationZ = 9.8; 
                stepCount = $steps; deviceState = $status; 
                isDeviceMoving = ($status -eq "WALKING");
                deviceFingerprint = $fingerprint; 
                biometricSignature = "BIO-SIG-$email";
                batteryLevel = 85; isScreenOn = $true;
                wifiNetworks = $wifi
            } | ConvertTo-Json

            Invoke-RestMethod -Uri "$baseUrl/attendance/heartbeat-enhanced" -Method Post -ContentType "application/json" -Headers @{ Authorization = "Bearer $($login.accessToken)" } -Body $pulse
            Write-Host "   [OK] $email -> $behavior" -ForegroundColor Gray
        } catch {
            $isAnomaly = $false
            if ($_.Exception.Response) {
                $statusBytes = $_.Exception.Response.StatusCode
                $errBody = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream()).ReadToEnd()
                
                # 🛡️ Handle Expected Security Anomalies
                if ($errBody -like "*Spoofing detected*" -or $errBody -like "*Security Violation*") {
                    Write-Host "   [SPOOF_DETECTED] $email -> Anomaly handled successfully" -ForegroundColor Cyan
                    $count_Anom++
                    $isAnomaly = $true
                } else {
                    Write-Host "   [FAIL] $email -> $errBody" -ForegroundColor DarkRed
                }
            } else {
                Write-Host "   [FAIL] $email -> $($_.Exception.Message)" -ForegroundColor DarkRed
            }
            
            if (-not $isAnomaly) { $count_Fail++ }
        }
    }
    Write-Host "   Done. Summary: [P:$count_P, A:$count_A, W:$count_W, ANOM:$count_Anom] | ACTUAL FAIL: $count_Fail" -ForegroundColor Cyan
}

Write-Host "--- COMMANDER SIMULATION COMPLETE ---" -ForegroundColor Cyan
