# test_mail.ps1
# -------------------------------------------------
# 1️⃣ Load the JWT secret from .env
# -------------------------------------------------
$secret = (Get-Content .env | Where-Object { $_ -match '^JWT_SECRET=' }) -replace 'JWT_SECRET=', ''

# -------------------------------------------------
# 2️⃣ Create a short‑lived JWT (5 min) using PowerShell (no Docker)
# -------------------------------------------------
$rawJwt = docker run --rm -e SECRET=$secret node:20-alpine sh -c "npm install -g jsonwebtoken >/dev/null && node -p `"require('jsonwebtoken').sign({sub:'test@example.com',exp: Math.floor(Date.now()/1000)+300}, process.env.SECRET)`""
Write-Host "Raw JWT output: $rawJwt"
$jwt = $rawJwt.Trim()

# -------------------------------------------------
# 3️⃣ Call the password‑reset endpoint via the API‑gateway
# -------------------------------------------------
curl.exe -i -X POST http://localhost:8080/api/notifications/account/password-reset -H "Content-Type: application/json" -H "Authorization: Bearer $jwt" -d "{\"recipientEmail\":\"test@example.com\",\"resetUrl\":\"https://example.com/reset?token=demo\",\"userName\":\"Demo User\",\"expireInMinutes\":15}"

# Optional sanity checks
Write-Host "\nGenerated JWT: $jwt"
if (-not $jwt) { Write-Host "NO JWT produced - inspect $rawJwt for errors." }
