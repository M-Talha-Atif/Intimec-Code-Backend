# Set passwords for test users via password reset API
$ErrorActionPreference = "Stop"

$AUTH_SERVICE_URL = "http://localhost:8090"

$users = @(
    @{ Email = "john.smith@company.com"; Password = "Password123!" },
    @{ Email = "jane.doe@company.com"; Password = "Password123!" },
    @{ Email = "bob.johnson@company.com"; Password = "Password123!" },
    @{ Email = "alice.williams@company.com"; Password = "Password123!" },
    @{ Email = "charlie.brown@company.com"; Password = "Password123!" }
)

Write-Host "Setting passwords for 5 test users..." -ForegroundColor Cyan
Write-Host ""

foreach ($user in $users) {
    Write-Host "Processing: $($user.Email)..." -ForegroundColor Yellow
    
    try {
        # Step 1: Request password reset
        $resetRequestBody = @{ email = $user.Email } | ConvertTo-Json -Compress
        $response = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/api/v1/auth/password/reset-request" `
            -Method POST `
            -ContentType "application/json" `
            -Body $resetRequestBody
        
        Write-Host "  Password reset requested" -ForegroundColor Green
        Start-Sleep -Seconds 1
        
        # Step 2: Get token from database
        $tokenQuery = "SELECT token FROM password_reset_token WHERE user_id = (SELECT id FROM users WHERE email = '$($user.Email)') LIMIT 1;"
        $token = psql -h localhost -U postgres -d intimec -A -t -c $tokenQuery 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  Failed to get token: $token" -ForegroundColor Red
            continue
        }
        
        $token = $token.Trim()
        if ([string]::IsNullOrWhiteSpace($token)) {
            Write-Host "  No token found" -ForegroundColor Red
            continue
        }
        
        Write-Host "  Token retrieved" -ForegroundColor Green
        
        # Step 3: Set new password using token
        $passwordResetBody = @{ newPassword = $user.Password } | ConvertTo-Json -Compress
        $resetResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/api/v1/auth/password/reset/$token" `
            -Method PUT `
            -ContentType "application/json" `
            -Body $passwordResetBody
        
        Write-Host "  Password set successfully!" -ForegroundColor Green
        
    } catch {
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails.Message) {
            Write-Host "  Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
        }
    }
    
    Write-Host ""
}

Write-Host "Done!" -ForegroundColor Green
