# ============================================================
# Create User Hierarchy Script
# Creates managers first, then employees with manager IDs
# ============================================================

$API_BASE = "http://localhost:8081/api/v1/identity/employee"
$AUTH_URL = "http://localhost:8081/api/v1/auth/login"

# Store created user IDs
$createdUsers = @{}
$script:AuthToken = $null

# ============================================================
# Login Function - Get JWT Token
# ============================================================
function Get-AuthToken {
    Write-Host "`nAuthenticating..." -ForegroundColor Yellow
    
    $loginData = @{
        identifier = "testadmin"
        password = "NewPassword123!"
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri $AUTH_URL -Method POST -Body $loginData -ContentType "application/json"
        
        # Extract token from response
        $token = $response.token
        if (-not $token) {
            $token = $response.accessToken
        }
        if (-not $token) {
            $token = $response.data.token
        }
        
        if ($token) {
            $script:AuthToken = $token
            Write-Host "Authentication successful!" -ForegroundColor Green
            return $true
        }
        else {
            Write-Host "No token in response: $($response | ConvertTo-Json)" -ForegroundColor Red
            return $false
        }
    }
    catch {
        Write-Host "Authentication FAILED: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Response: $($_.ErrorDetails.Message)" -ForegroundColor Red
        return $false
    }
}

function Create-Employee {
    param(
        [string]$Key,
        [hashtable]$Data
    )
    
    $json = $Data | ConvertTo-Json -Depth 10
    
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "Creating: $($Data.firstName) $($Data.lastName) ($($Data.jobTitle))" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    
    $headers = @{
        "Authorization" = "Bearer $script:AuthToken"
        "Content-Type" = "application/json"
    }
    
    try {
        $response = Invoke-RestMethod -Uri $API_BASE -Method POST -Body $json -Headers $headers
        
        # Extract ID from response (adjust based on actual response format)
        $id = $response.id
        if (-not $id) {
            $id = $response.data.id
        }
        if (-not $id) {
            $id = $response
        }
        
        $createdUsers[$Key] = $id
        
        Write-Host "SUCCESS: Created with ID: $id" -ForegroundColor Green
        Write-Host "Email: $($Data.email)" -ForegroundColor Gray
        Write-Host "Username: $($Data.username)" -ForegroundColor Gray
        if ($Data.managerId) {
            Write-Host "Manager ID: $($Data.managerId)" -ForegroundColor Gray
        }
        
        return $id
    }
    catch {
        Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Response: $($_.ErrorDetails.Message)" -ForegroundColor Red
        return $null
    }
}

Write-Host @"

================================================================
           USER HIERARCHY CREATION SCRIPT                     
                                                              
  This script will create:                                    
  - 2 Top-level managers (no manager)                         
  - 4 Employees reporting to those managers                   
================================================================

"@ -ForegroundColor Yellow

# ============================================================
# STEP 0: Authenticate
# ============================================================

Write-Host ">>> STEP 0: Authentication <<<" -ForegroundColor Magenta
Write-Host "=============================================" -ForegroundColor Magenta

$authenticated = Get-AuthToken
if (-not $authenticated) {
    Write-Host "`nFailed to authenticate. Exiting." -ForegroundColor Red
    exit 1
}

# ============================================================
# STEP 1: Create Top-Level Managers (No Manager ID)
# ============================================================

Write-Host "`n`n>>> STEP 1: Creating Top-Level Managers <<<" -ForegroundColor Magenta
Write-Host "=============================================" -ForegroundColor Magenta

# CEO - Top level
$ceoId = Create-Employee -Key "CEO" -Data @{
    email = "ceo@intimec.com"
    username = "ceo"
    password = "SecurePass123!"
    firstName = "John"
    lastName = "Executive"
    employeeId = "EMP-001"
    jobTitle = "Chief Executive Officer"
    department = "Executive"
    hireDate = "2020-01-01T00:00:00.000+00:00"
    status = "ACTIVE"
    extendedAttributes = @{
        level = "C-Suite"
        office = "HQ"
    }
}

Start-Sleep -Milliseconds 500

# CTO - Reports to no one (or CEO if you want)
$ctoId = Create-Employee -Key "CTO" -Data @{
    email = "cto@intimec.com"
    username = "cto"
    password = "SecurePass123!"
    firstName = "Sarah"
    lastName = "Tech"
    employeeId = "EMP-002"
    jobTitle = "Chief Technology Officer"
    department = "Technology"
    hireDate = "2020-03-15T00:00:00.000+00:00"
    status = "ACTIVE"
    extendedAttributes = @{
        level = "C-Suite"
        office = "HQ"
    }
}

Start-Sleep -Milliseconds 500

# ============================================================
# STEP 2: Create Department Managers (Report to CTO)
# ============================================================

Write-Host "`n`n>>> STEP 2: Creating Department Managers <<<" -ForegroundColor Magenta
Write-Host "=============================================" -ForegroundColor Magenta

if ($ctoId) {
    # Engineering Manager
    $engManagerId = Create-Employee -Key "ENG_MANAGER" -Data @{
        email = "eng.manager@intimec.com"
        username = "eng_manager"
        password = "SecurePass123!"
        firstName = "Alice"
        lastName = "Engineering"
        employeeId = "EMP-003"
        jobTitle = "Engineering Manager"
        department = "Engineering"
        managerId = $ctoId
        hireDate = "2021-06-01T00:00:00.000+00:00"
        status = "ACTIVE"
        extendedAttributes = @{
            level = "Manager"
            team = "Platform"
        }
    }
    
    Start-Sleep -Milliseconds 500
    
    # QA Manager
    $qaManagerId = Create-Employee -Key "QA_MANAGER" -Data @{
        email = "qa.manager@intimec.com"
        username = "qa_manager"
        password = "SecurePass123!"
        firstName = "Bob"
        lastName = "Quality"
        employeeId = "EMP-004"
        jobTitle = "QA Manager"
        department = "Quality Assurance"
        managerId = $ctoId
        hireDate = "2021-09-01T00:00:00.000+00:00"
        status = "ACTIVE"
        extendedAttributes = @{
            level = "Manager"
            team = "QA"
        }
    }
}
else {
    Write-Host "Skipping department managers - CTO not created" -ForegroundColor Yellow
}

Start-Sleep -Milliseconds 500

# ============================================================
# STEP 3: Create Regular Employees (Report to Managers)
# ============================================================

Write-Host "`n`n>>> STEP 3: Creating Employees <<<" -ForegroundColor Magenta
Write-Host "=============================================" -ForegroundColor Magenta

if ($createdUsers["ENG_MANAGER"]) {
    # Developer 1 - Reports to Engineering Manager
    Create-Employee -Key "DEV1" -Data @{
        email = "developer1@intimec.com"
        username = "developer1"
        password = "SecurePass123!"
        firstName = "Charlie"
        lastName = "Developer"
        employeeId = "EMP-005"
        jobTitle = "Senior Software Developer"
        department = "Engineering"
        managerId = $createdUsers["ENG_MANAGER"]
        hireDate = "2023-01-15T00:00:00.000+00:00"
        status = "ACTIVE"
        extendedAttributes = @{
            level = "Senior"
            team = "Backend"
            skills = "Java,Spring,PostgreSQL"
        }
    }
    
    Start-Sleep -Milliseconds 500
    
    # Developer 2 - Reports to Engineering Manager
    Create-Employee -Key "DEV2" -Data @{
        email = "developer2@intimec.com"
        username = "developer2"
        password = "SecurePass123!"
        firstName = "Diana"
        lastName = "Coder"
        employeeId = "EMP-006"
        jobTitle = "Software Developer"
        department = "Engineering"
        managerId = $createdUsers["ENG_MANAGER"]
        hireDate = "2024-03-01T00:00:00.000+00:00"
        status = "ACTIVE"
        extendedAttributes = @{
            level = "Mid"
            team = "Frontend"
            skills = "React,TypeScript,Next.js"
        }
    }
}

Start-Sleep -Milliseconds 500

if ($createdUsers["QA_MANAGER"]) {
    # QA Engineer 1 - Reports to QA Manager
    Create-Employee -Key "QA1" -Data @{
        email = "qa.engineer1@intimec.com"
        username = "qa_engineer1"
        password = "SecurePass123!"
        firstName = "Eve"
        lastName = "Tester"
        employeeId = "EMP-007"
        jobTitle = "QA Engineer"
        department = "Quality Assurance"
        managerId = $createdUsers["QA_MANAGER"]
        hireDate = "2023-06-01T00:00:00.000+00:00"
        status = "ACTIVE"
        extendedAttributes = @{
            level = "Mid"
            team = "QA"
            skills = "Selenium,Cypress,API Testing"
        }
    }
    
    Start-Sleep -Milliseconds 500
    
    # QA Engineer 2 - Reports to QA Manager
    Create-Employee -Key "QA2" -Data @{
        email = "qa.engineer2@intimec.com"
        username = "qa_engineer2"
        password = "SecurePass123!"
        firstName = "Frank"
        lastName = "Validator"
        employeeId = "EMP-008"
        jobTitle = "Junior QA Engineer"
        department = "Quality Assurance"
        managerId = $createdUsers["QA_MANAGER"]
        hireDate = "2025-01-10T00:00:00.000+00:00"
        status = "ACTIVE"
        extendedAttributes = @{
            level = "Junior"
            team = "QA"
            skills = "Manual Testing,Bug Tracking"
        }
    }
}

# ============================================================
# SUMMARY
# ============================================================

Write-Host @"

╔══════════════════════════════════════════════════════════════╗
║                    CREATION SUMMARY                          ║
╚══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Green

Write-Host "`nCreated Users:" -ForegroundColor Cyan
Write-Host "─────────────────────────────────────────────────────" -ForegroundColor Gray

foreach ($key in $createdUsers.Keys) {
    Write-Host "  $key : $($createdUsers[$key])" -ForegroundColor White
}

Write-Host "`n`nOrganization Hierarchy:" -ForegroundColor Cyan
Write-Host "─────────────────────────────────────────────────────" -ForegroundColor Gray
Write-Host @"
  CEO (John Executive)
  │
  └── CTO (Sarah Tech)
      │
      ├── Engineering Manager (Alice Engineering)
      │   ├── Developer 1 (Charlie Developer)
      │   └── Developer 2 (Diana Coder)
      │
      └── QA Manager (Bob Quality)
          ├── QA Engineer 1 (Eve Tester)
          └── QA Engineer 2 (Frank Validator)
"@ -ForegroundColor White

Write-Host "`n`n[!] IMPORTANT NOTES:" -ForegroundColor Yellow
Write-Host "-----------------------------------------------------" -ForegroundColor Gray
Write-Host "1. Users are created in auth-service via Kafka (async)" -ForegroundColor White
Write-Host "2. Passwords are NOT synced - users have no password initially" -ForegroundColor White
Write-Host "3. Users need password reset before they can login" -ForegroundColor White
Write-Host "4. Save the IDs above for testing workflows!" -ForegroundColor White

Write-Host "`n`nDone!" -ForegroundColor Green
