# 📥 Smart Attendance - Database Backup Script
# This script creates a timestamped backup of your local database.
# Recommendation: Run this every night at 2:00 AM using Windows Task Scheduler.

$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm"
$backupDir = "./backups"
$backupFile = "$backupDir/smart_attendance_backup_$timestamp.sql"

if (!(Test-Path $backupDir)) {
    New-Item -ItemType Directory -Path $backupDir
}

Write-Host "[*] Starting Database Backup..." -ForegroundColor Green

# Use PostgreSQL pg_dump utility
# Note: Ensure PostgreSQL/bin is in your System PATH
try {
    $env:PGPASSWORD = "Pragathi@2105" # Match the password in application-local.properties
    & pg_dump -U postgres -d smart_attendance -f $backupFile
    Write-Host "[OK] Backup saved to: $backupFile" -ForegroundColor Cyan
} catch {
    Write-Host "[!] Error: Failed to create backup. Ensure 'pg_dump' is in your PATH." -ForegroundColor Red
}

Write-Host "--- Backup Process Complete ---"
