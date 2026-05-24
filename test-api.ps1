# Task 1.4 API Testing Script for PowerShell
# Usage: .\test-api.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Task 1.4: Repository CRUD API Testing" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Set your JWT token here
$JWT_TOKEN = Read-Host "Enter your JWT token"

if ([string]::IsNullOrWhiteSpace($JWT_TOKEN)) {
    Write-Host "Error: JWT token is required!" -ForegroundColor Red
    exit 1
}

$BASE_URL = "http://localhost:8080/api"

# Test 1: Verify authentication
Write-Host "[Test 1] Testing authentication with /api/auth/me..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$BASE_URL/auth/me" -Headers @{"Authorization"="Bearer $JWT_TOKEN"} -UseBasicParsing
    Write-Host "✓ Authentication successful!" -ForegroundColor Green
    Write-Host $response.Content
} catch {
    Write-Host "✗ Authentication failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Test 2: List repositories (should be empty)
Write-Host "[Test 2] Listing repositories..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$BASE_URL/repos" -Headers @{"Authorization"="Bearer $JWT_TOKEN"} -UseBasicParsing
    Write-Host "✓ Success!" -ForegroundColor Green
    Write-Host $response.Content
} catch {
    Write-Host "✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 3: Connect a repository
Write-Host "[Test 3] Connecting repository: octocat/Hello-World..." -ForegroundColor Yellow
try {
    $body = @{
        repoFullName = "octocat/Hello-World"
        branch = "master"
    } | ConvertTo-Json

    $response = Invoke-WebRequest -Uri "$BASE_URL/repos" `
        -Method POST `
        -Headers @{
            "Authorization"="Bearer $JWT_TOKEN"
            "Content-Type"="application/json"
        } `
        -Body $body `
        -UseBasicParsing

    Write-Host "✓ Repository connected!" -ForegroundColor Green
    $responseData = $response.Content | ConvertFrom-Json
    Write-Host $response.Content
    
    # Save repo ID for later tests
    $REPO_ID = $responseData.data.id
    Write-Host ""
    Write-Host "Repo ID: $REPO_ID" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
}
Write-Host ""

# Test 4: List repositories again
Write-Host "[Test 4] Listing repositories again..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$BASE_URL/repos" -Headers @{"Authorization"="Bearer $JWT_TOKEN"} -UseBasicParsing
    Write-Host "✓ Success!" -ForegroundColor Green
    Write-Host $response.Content
} catch {
    Write-Host "✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 5: Get specific repository
if ($REPO_ID) {
    Write-Host "[Test 5] Getting repository details..." -ForegroundColor Yellow
    try {
        $response = Invoke-WebRequest -Uri "$BASE_URL/repos/$REPO_ID" -Headers @{"Authorization"="Bearer $JWT_TOKEN"} -UseBasicParsing
        Write-Host "✓ Success!" -ForegroundColor Green
        Write-Host $response.Content
    } catch {
        Write-Host "✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""

    # Test 6: Trigger reindex
    Write-Host "[Test 6] Triggering reindex..." -ForegroundColor Yellow
    try {
        $response = Invoke-WebRequest -Uri "$BASE_URL/repos/$REPO_ID/reindex" `
            -Method POST `
            -Headers @{"Authorization"="Bearer $JWT_TOKEN"} `
            -UseBasicParsing
        Write-Host "✓ Reindex triggered!" -ForegroundColor Green
        Write-Host $response.Content
    } catch {
        Write-Host "✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""

    # Test 7: Disconnect repository
    $disconnect = Read-Host "Do you want to disconnect the repository? (y/n)"
    if ($disconnect -eq "y") {
        Write-Host "[Test 7] Disconnecting repository..." -ForegroundColor Yellow
        try {
            $response = Invoke-WebRequest -Uri "$BASE_URL/repos/$REPO_ID" `
                -Method DELETE `
                -Headers @{"Authorization"="Bearer $JWT_TOKEN"} `
                -UseBasicParsing
            Write-Host "✓ Repository disconnected!" -ForegroundColor Green
            Write-Host $response.Content
        } catch {
            Write-Host "✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
