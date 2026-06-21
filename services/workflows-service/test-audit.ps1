# Test script to create a workflow and trigger audit event
$body = @{
    name = "Test Audit Workflow"
    description = "Testing audit logging system"
    nodes = @(
        @{
            id = "node1"
            type = "DECISION"
            name = "Test Node"
            config = @{}
            position = @{
                x = 100
                y = 100
            }
        }
    )
    edges = @()
    variables = @{}
} | ConvertTo-Json -Depth 10

Write-Host "Creating workflow to test audit logging..." -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8098/api/workflows" -Method Post -Body $body -ContentType "application/json"
    Write-Host "SUCCESS! Workflow created:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10
    Write-Host "`nNow check:" -ForegroundColor Yellow
    Write-Host "1. Kafka UI at http://localhost:9093" -ForegroundColor Yellow
    Write-Host "2. Look for audit-log topic" -ForegroundColor Yellow
    Write-Host "3. Should see audit-workflow-created event" -ForegroundColor Yellow
} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
    Write-Host $_.Exception.Response.StatusCode -ForegroundColor Red
}
