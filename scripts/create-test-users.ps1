# Create 5 Test Users for Workflow Testing
# Uses password reset API to avoid BCrypt hashing issues

$ErrorActionPreference = "Stop"

$AUTH_SERVICE_URL = "http://localhost:8090"
$DB_HOST = "localhost"
$DB_USER = "postgres"
$DB_NAME = "intimec"
$TENANT_ID = "default"  # Changed from default-tenant to match existing password policy
$USER_ROLE = "SUPER_ADMIN"  # SUPER_ADMIN gives access to all pages

# Prompt for PostgreSQL password if not set
if (-not $env:PGPASSWORD) {
    $securePassword = Read-Host "Enter PostgreSQL password for user '$DB_USER'" -AsSecureString
    $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $env:PGPASSWORD = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
}

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Creating 5 Test Users for Workflow Testing" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

$users = @(
    @{ Username = "john.smith"; Email = "john.smith@company.com"; FirstName = "John"; LastName = "Smith"; DisplayName = "John Smith"; Password = "Password123!"; Role = "Manager" },
    @{ Username = "jane.doe"; Email = "jane.doe@company.com"; FirstName = "Jane"; LastName = "Doe"; DisplayName = "Jane Doe"; Password = "Password123!"; Role = "Team Lead" },
    @{ Username = "bob.johnson"; Email = "bob.johnson@company.com"; FirstName = "Bob"; LastName = "Johnson"; DisplayName = "Bob Johnson"; Password = "Password123!"; Role = "Security Admin" },
    @{ Username = "alice.williams"; Email = "alice.williams@company.com"; FirstName = "Alice"; LastName = "Williams"; DisplayName = "Alice Williams"; Password = "Password123!"; Role = "Developer" },
    @{ Username = "charlie.brown"; Email = "charlie.brown@company.com"; FirstName = "Charlie"; LastName = "Brown"; DisplayName = "Charlie Brown"; Password = "Password123!"; Role = "Executive" }
)

function Invoke-SQL {
    param([string]$Query)
    $result = psql -h $DB_HOST -U $DB_USER -d $DB_NAME -A -t -c $Query 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "SQL Error: $result" -ForegroundColor Red
        throw "SQL command failed"
    }
    if ($result) {
        return $result.Trim()
    }
    return ""
}

Write-Host "Step 1: Creating users in database..." -ForegroundColor Cyan
Write-Host ""

foreach ($user in $users) {
    Write-Host "Creating user: $($user.Username)..." -ForegroundColor Yellow
    
    # Check if user exists
    $checkSql = "SELECT id FROM users WHERE email = '$($user.Email)' LIMIT 1;"
    $existingId = Invoke-SQL -Query $checkSql
    
    if ($existingId -and $existingId.Length -gt 0) {
        Write-Host "  User already exists, using ID: $existingId" -ForegroundColor Cyan
        $userId = $existingId
        # Update user to ensure active status and reset failed login attempts
        $updateSql = "UPDATE users SET username = '$($user.Username)', first_name = '$($user.FirstName)', last_name = '$($user.LastName)', display_name = '$($user.DisplayName)', status = 'ACTIVE', failed_login_attempts = 0, updated_at = NOW() WHERE id = '$userId'::uuid;"
        Invoke-SQL -Query $updateSql | Out-Null
    } else {
        $sql = "INSERT INTO users (id, email, username, status, tenant_id, created_at, updated_at, first_name, last_name, display_name, mfa_enabled, is_password_expired, failed_login_attempts, profile_data) VALUES (gen_random_uuid(), '$($user.Email)', '$($user.Username)', 'ACTIVE', '$TENANT_ID', NOW(), NOW(), '$($user.FirstName)', '$($user.LastName)', '$($user.DisplayName)', false, false, 0, '{}') RETURNING id;"
        
        try {
            $insertResult = Invoke-SQL -Query $sql
            # Extract UUID from result (may contain "INSERT 0 1" text)
            if ($insertResult -match '([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})') {
                $userId = $matches[1]
            } else {
                throw "Failed to extract user ID from insert result: '$insertResult'"
            }
        } catch {
            Write-Host "  Insert failed, trying to fetch existing user..." -ForegroundColor Yellow
            Start-Sleep -Milliseconds 500
            $userId = Invoke-SQL -Query $checkSql
            if (-not $userId -or $userId.Length -eq 0) {
                throw "Failed to create or find user: $_"
            }
        }
    }
    
    $user.Id = $userId
    
    # Check if user already has any role
    $checkAnyRoleSql = "SELECT id FROM users_role WHERE user_id = '$userId'::uuid LIMIT 1;"
    $existingAnyRole = Invoke-SQL -Query $checkAnyRoleSql
    
    if ($existingAnyRole -and $existingAnyRole.Length -gt 0) {
        # Update existing role to SUPER_ADMIN
        $updateRoleSql = "UPDATE users_role SET role = '$USER_ROLE' WHERE user_id = '$userId'::uuid;"
        Invoke-SQL -Query $updateRoleSql | Out-Null
    } else {
        # Insert new role
        $roleSql = "INSERT INTO users_role (id, user_id, role) VALUES (gen_random_uuid(), '$userId'::uuid, '$USER_ROLE');"
        Invoke-SQL -Query $roleSql | Out-Null
    }
    
    Write-Host "  [OK] Created user ID: $userId" -ForegroundColor Green
    Write-Host ""
}

Write-Host "Step 2: Setting passwords via API..." -ForegroundColor Cyan
Write-Host ""

$successCount = 0
foreach ($user in $users) {
    Write-Host "Setting password for: $($user.Username)..." -ForegroundColor Yellow
    
    try {
        $resetRequest = @{ email = $user.Email } | ConvertTo-Json
        Invoke-WebRequest -Uri "$AUTH_SERVICE_URL/api/v1/auth/password/reset-request" -Method POST -Headers @{"Content-Type"="application/json"} -Body $resetRequest -ErrorAction Stop | Out-Null
        Write-Host "  Password reset requested" -ForegroundColor Green
        
        Start-Sleep -Seconds 1
        
        $tokenQuery = "SELECT token FROM password_reset_token WHERE user_id = '$($user.Id)'::uuid LIMIT 1;"
        $token = (Invoke-SQL -Query $tokenQuery).Trim()
        
        if ([string]::IsNullOrWhiteSpace($token)) {
            Write-Host "  No reset token found" -ForegroundColor Red
            continue
        }
        
        Write-Host "  Reset token retrieved" -ForegroundColor Green
        
        $passwordRequest = @{ newPassword = $user.Password } | ConvertTo-Json
        Invoke-WebRequest -Uri "$AUTH_SERVICE_URL/api/v1/auth/password/reset/$token" -Method PUT -Headers @{"Content-Type"="application/json"} -Body $passwordRequest -ErrorAction Stop | Out-Null
        
        Write-Host "  Password set successfully" -ForegroundColor Green
        $successCount++
        
    } catch {
        Write-Host "  API Error: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Write-Host ""
    Start-Sleep -Seconds 1
}

Write-Host "Step 3: Testing logins..." -ForegroundColor Cyan
Write-Host ""

$loginSuccessCount = 0
foreach ($user in $users) {
    Write-Host "Testing login for: $($user.Username)..." -ForegroundColor Yellow
    
    try {
        $loginRequest = @{ identifier = $user.Username; password = $user.Password } | ConvertTo-Json
        $response = Invoke-WebRequest -Uri "$AUTH_SERVICE_URL/api/v1/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body $loginRequest -ErrorAction Stop
        
        Write-Host "  Login successful!" -ForegroundColor Green
        $loginSuccessCount++
        
    } catch {
        Write-Host "  Login failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Write-Host ""
    Start-Sleep -Seconds 1
}

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Summary" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Users created in database: $($users.Count)" -ForegroundColor White
Write-Host "Passwords set successfully: $successCount" -ForegroundColor Green
Write-Host "Login tests passed: $loginSuccessCount" -ForegroundColor Green
Write-Host ""
Write-Host "Test User Credentials:" -ForegroundColor Cyan
Write-Host "-----------------------------" -ForegroundColor Cyan
foreach ($user in $users) {
    Write-Host "$($user.Username.PadRight(20)) / Password123!  ($($user.Role))" -ForegroundColor White
}
Write-Host ""
Write-Host "User IDs for workflow configuration:" -ForegroundColor Cyan
Write-Host "-----------------------------" -ForegroundColor Cyan
foreach ($user in $users) {
    if ($user.Id) {
        Write-Host "$($user.DisplayName.PadRight(20)) : $($user.Id)" -ForegroundColor White
    }
}
Write-Host ""
Write-Host "Done!" -ForegroundColor Green
