# verify-vaccination-reminder-email.ps1
# End-to-end check for vaccination reminder email sending.

param(
    [string]$BaseUrl = "http://localhost:8081",
    [string]$DoctorEmail = "petcore26@gmail.com",
    [string]$DoctorPassword = "zgfqtnqzpdcpbblx",
    [int]$PetId = 1,

    # Choose run date and reminder window:
    # - If ReminderType=WEEK_BEFORE, validUntil = RunDate + 7
    # - If ReminderType=DAY_BEFORE,  validUntil = RunDate + 1
    [datetime]$RunDate = "2026-04-19",
    [ValidateSet("WEEK_BEFORE","DAY_BEFORE")]
    [string]$ReminderType = "WEEK_BEFORE",

    [string]$VaccineName = "Rabies",
    [string]$MySqlUser = "root",
    [string]$MySqlPassword = "your_mysql_password",
    [string]$MySqlDb = "petclinic",
    [string]$MySqlExe = "mysql"
)

$ErrorActionPreference = "Stop"

Write-Host "== Step 0: Pre-check ==" -ForegroundColor Cyan
if ($ReminderType -eq "WEEK_BEFORE") {
    $validUntil = $RunDate.AddDays(7).ToString("yyyy-MM-dd")
    $label = "1 week before"
} else {
    $validUntil = $RunDate.AddDays(1).ToString("yyyy-MM-dd")
    $label = "1 day before"
}
$givenAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")

Write-Host "RunDate       : $($RunDate.ToString("yyyy-MM-dd"))"
Write-Host "ReminderType  : $ReminderType ($label)"
Write-Host "ValidUntil    : $validUntil"
Write-Host "GivenAt       : $givenAt"
Write-Host "PetId         : $PetId"
Write-Host ""

Write-Host "== Step 1: Login and keep session cookie ==" -ForegroundColor Cyan
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginBody = @{
    email = $DoctorEmail
    password = $DoctorPassword
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/api/auth/login" `
    -WebSession $session `
    -ContentType "application/json" `
    -Body $loginBody

Write-Host "Login response: $loginResponse"
Write-Host ""

Write-Host "== Step 2: Create vaccination ==" -ForegroundColor Cyan
$vaccinationBody = @{
    vaccineName = $VaccineName
    givenAt = $givenAt
    validUntil = $validUntil
    notes = "Reminder email e2e test $(Get-Date -Format s)"
} | ConvertTo-Json

$createResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/api/pets/$PetId/vaccinations" `
    -WebSession $session `
    -ContentType "application/json" `
    -Body $vaccinationBody

Write-Host "Created vaccination id: $($createResponse.id)"
Write-Host "Created vaccination validUntil: $($createResponse.validUntil)"
Write-Host ""

Write-Host "== Step 3: Trigger reminder run manually ==" -ForegroundColor Cyan
$runDateIso = $RunDate.ToString("yyyy-MM-dd")
$triggerResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/api/notifications/vaccination-reminders/send-now?runDate=$runDateIso" `
    -WebSession $session

Write-Host "Reminder run response:"
$triggerResponse | ConvertTo-Json -Depth 5
Write-Host ""

Write-Host "== Step 4: Check reminder status endpoint ==" -ForegroundColor Cyan
$statusResponse = Invoke-RestMethod `
    -Method Get `
    -Uri "$BaseUrl/api/notifications/vaccination-reminders/status" `
    -WebSession $session

$statusResponse | ConvertTo-Json -Depth 5
Write-Host ""

Write-Host "== Step 5: Query DB dispatch table (optional but recommended) ==" -ForegroundColor Cyan
# Requires mysql cli in PATH. If not in PATH, set -MySqlExe to full path.
$sql = @"
SELECT dispatch_id, vaccination_id, reminder_type, status, reminder_date, created_at
FROM vaccination_reminder_dispatch
ORDER BY dispatch_id DESC
LIMIT 15;
"@

& $MySqlExe -u $MySqlUser -p"$MySqlPassword" -D $MySqlDb -e $sql

Write-Host ""
Write-Host "== Done ==" -ForegroundColor Green
Write-Host "Now check recipient inbox/spam for 'Pet Clinic - Vaccination Validity Reminder'."
Write-Host "Expected run result: sentCount >= 1 for matching window."
