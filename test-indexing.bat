@echo off
setlocal enabledelayedexpansion

echo ========================================
echo   Indexing Flow Test Script
echo ========================================
echo.

REM Check if backend is running
echo [1/7] Checking if backend is running...
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Backend is running
) else (
    echo [ERROR] Backend is not running!
    echo Please start it with: cd backend ^&^& mvnw.cmd spring-boot:run
    exit /b 1
)
echo.

REM Check PostgreSQL
echo [2/7] Checking PostgreSQL...
docker ps | findstr postgres >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] PostgreSQL is running
) else (
    echo [ERROR] PostgreSQL is not running!
    echo Please start it with: docker-compose up -d postgres
    exit /b 1
)
echo.

REM Check Redis
echo [3/7] Checking Redis...
docker ps | findstr redis >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Redis is running
) else (
    echo [ERROR] Redis is not running!
    echo Please start it with: docker-compose up -d redis
    exit /b 1
)
echo.

echo ========================================
echo   All prerequisites are ready!
echo ========================================
echo.
echo Next steps:
echo.
echo 1. Authenticate with GitHub:
echo    Open: http://localhost:8080/api/auth/github
echo.
echo 2. Copy the JWT token from the response
echo.
echo 3. Connect a test repository:
echo.
echo    curl -X POST http://localhost:8080/api/repos ^
echo      -H "Content-Type: application/json" ^
echo      -H "Authorization: Bearer YOUR_JWT_TOKEN" ^
echo      -d "{\"fullName\":\"octocat/Hello-World\",\"defaultBranch\":\"master\"}"
echo.
echo 4. Watch the backend logs for indexing progress
echo.
echo 5. Check the result:
echo.
echo    curl http://localhost:8080/api/repos ^
echo      -H "Authorization: Bearer YOUR_JWT_TOKEN"
echo.
echo ========================================
echo   See INDEXING-TEST-GUIDE.md for details
echo ========================================

pause
