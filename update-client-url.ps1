param (
    [string]$CloudflareUrl
)

if ([string]::IsNullOrWhiteSpace($CloudflareUrl)) {
    $CloudflareUrl = Read-Host "Please enter the Cloudflare Tunnel URL (e.g., https://xxx.trycloudflare.com)"
}

if ([string]::IsNullOrWhiteSpace($CloudflareUrl)) {
    Write-Error "No URL provided. Aborting."
    exit
}

# Ensure URL has https prefix if missing
if (!$CloudflareUrl.StartsWith("http")) {
    $CloudflareUrl = "https://" + $CloudflareUrl
}
$CloudflareUrl = $CloudflareUrl.TrimEnd("/")


Write-Host "Updating Frontend to use New Cloudflare URL: $CloudflareUrl" -ForegroundColor Cyan

$envFiles = @("frontend\web-dashboard\.env.production", "frontend\web-dashboard\.env.local")

foreach ($envFile in $envFiles) {
    if (Test-Path $envFile) {
        Write-Host "[*] Updating $envFile ..." -ForegroundColor Gray
        $envContent = Get-Content $envFile
        $newEnv = @()

        foreach ($line in $envContent) {
            if ($line.StartsWith("NEXT_PUBLIC_API_URL=")) {
                $newEnv += "NEXT_PUBLIC_API_URL=$CloudflareUrl"
            } elseif ($line.StartsWith("NEXT_PUBLIC_WS_URL=")) {
                # Convert http/https to ws/wss
                $wsUrl = $CloudflareUrl -replace "^http", "ws"
                if ($wsUrl.EndsWith("/")) {
                    $wsUrl = $wsUrl.TrimEnd("/")
                }
                $newEnv += "NEXT_PUBLIC_WS_URL=$wsUrl/ws"
            } else {
                $newEnv += $line
            }
        }
        $newEnv | Set-Content $envFile
    }
}

Write-Host "Success! All frontend environment files updated." -ForegroundColor Green


Write-Host "NOTE: You must rebuild the frontend for this to take effect." -ForegroundColor Yellow
Write-Host "Run: cd frontend\web-dashboard; pnpm run build" -ForegroundColor Yellow
