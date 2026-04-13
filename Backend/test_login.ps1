[System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }
$baseUrl = "https://localhost:8443/api/v1"
$body = @{ email = "super.admin@smartattendence.com"; password = "Pragathi@2105" } | ConvertTo-Json
try {
    $res = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $body
    "SUCCESS token: $($res.accessToken)" | Out-File -FilePath "login_test.log"
} catch {
    "FAIL: $($_.Exception.Message)" | Out-File -FilePath "login_test.log"
}
