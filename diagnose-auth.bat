@echo off
echo ========================================
echo Diagnosing Authentication Issues
echo ========================================
echo.

echo 1. Checking if backend is running...
curl -s http://localhost:8080/actuator/health
echo.
echo.

echo 2. Checking if PostgreSQL is running...
psql -U postgres -d codebaseqa -c "SELECT 1;" 2>nul
if %errorlevel% equ 0 (
    echo PostgreSQL is running
) else (
    echo ERROR: PostgreSQL is not accessible
)
echo.

echo 3. Checking if Redis is running...
redis-cli ping 2>nul
if %errorlevel% equ 0 (
    echo Redis is running
) else (
    echo ERROR: Redis is not accessible
)
echo.

echo 4. Testing GitHub OAuth redirect...
echo Expected redirect URI: http://localhost:8080/api/auth/github/callback
echo.
echo Please verify in your GitHub OAuth App settings:
echo https://github.com/settings/developers
echo.

echo 5. Checking backend logs...
echo Please check the Spring Boot console for errors
echo Look for lines containing "GitHub OAuth failed" or "Failed to exchange code"
echo.

pause
