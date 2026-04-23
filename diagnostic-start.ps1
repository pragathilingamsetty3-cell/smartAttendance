# 🛡️ Secure Startup Diagnostic
Write-Host "[*] Diagnostic: Starting Backend..." -ForegroundColor Yellow

# Load .env variables into process memory
if (Test-Path "Backend\.env") {
    Get-Content "Backend\.env" | ForEach-Object {
        $line = $_.Trim()
        if ($line -and !$line.StartsWith("#") -and $line.Contains("=")) {
            $name, $value = $line -split '=', 2
            [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), "Process")
        }
    }
}

$jarPath = Get-ChildItem -Path "Backend\target\*.jar" | Select-Object -First 1
# Run Java directly and capture the first 50 lines of output
java -jar "$($jarPath.FullName)" --spring.profiles.active=local --server.port=10000 | Select-Object -First 50
