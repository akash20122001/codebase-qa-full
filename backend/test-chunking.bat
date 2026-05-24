@echo off
echo ========================================
echo Testing Chunking Service (Task 2.1)
echo ========================================
echo.

cd /d "%~dp0"

echo Running tests...
echo.

call mvnw.cmd test -Dtest=ChunkingServiceTest

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo ✅ All tests passed!
    echo ========================================
) else (
    echo.
    echo ========================================
    echo ❌ Tests failed!
    echo ========================================
)

pause
