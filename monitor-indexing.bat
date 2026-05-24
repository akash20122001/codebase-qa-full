@echo off
setlocal enabledelayedexpansion

echo ========================================
echo   Indexing Progress Monitor
echo ========================================
echo.

if "%1"=="" (
    echo Usage: monitor-indexing.bat YOUR_JWT_TOKEN
    echo.
    echo Example:
    echo   monitor-indexing.bat eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
    exit /b 1
)

set JWT_TOKEN=%1

:loop
cls
echo ========================================
echo   Indexing Status - %date% %time%
echo ========================================
echo.

REM Get all repos
curl -s http://localhost:8080/api/repos -H "Authorization: Bearer %JWT_TOKEN%" > temp_repos.json

REM Display repos (requires jq or manual parsing)
echo Repositories:
type temp_repos.json
echo.

echo ----------------------------------------
echo Press Ctrl+C to stop monitoring
echo Refreshing in 5 seconds...
echo ----------------------------------------

timeout /t 5 /nobreak >nul
goto loop
