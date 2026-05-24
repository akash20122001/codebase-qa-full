@echo off
echo ==========================================
echo Checking Prerequisites for Task 1.4
echo ==========================================
echo.

echo [1/4] Checking Docker...
docker --version >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Docker is installed
    docker --version
) else (
    echo [ERROR] Docker is not installed or not in PATH
)
echo.

echo [2/4] Checking Java...
java -version >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Java is installed
    java -version 2>&1 | findstr "version"
) else (
    echo [ERROR] Java is not installed or not in PATH
)
echo.

echo [3/4] Checking Maven...
cd backend
call mvnw.cmd --version >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Maven wrapper is available
) else (
    echo [ERROR] Maven wrapper not found
)
cd ..
echo.

echo [4/4] Checking curl...
curl --version >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] curl is installed
) else (
    echo [WARNING] curl not found - you can use Postman instead
)
echo.

echo ==========================================
echo Prerequisites check complete
echo ==========================================
pause
