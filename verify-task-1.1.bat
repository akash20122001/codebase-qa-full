@echo off
echo ==========================================
echo Task 1.1 Verification Script (Windows)
echo ==========================================
echo.

REM Check Java version
echo 1. Checking Java version...
java -version 2>&1 | findstr /C:"version"
if %errorlevel% equ 0 (
    echo    [OK] Java is installed
) else (
    echo    [ERROR] Java is not installed or not in PATH
    exit /b 1
)
echo.

REM Check Maven
echo 2. Checking Maven...
mvn -version 2>&1 | findstr /C:"Apache Maven"
if %errorlevel% equ 0 (
    echo    [OK] Maven is installed
) else (
    echo    [ERROR] Maven is not installed or not in PATH
    exit /b 1
)
echo.

REM Check Docker
echo 3. Checking Docker...
docker --version
if %errorlevel% equ 0 (
    echo    [OK] Docker is installed
) else (
    echo    [ERROR] Docker is not installed or not in PATH
    exit /b 1
)
echo.

REM Check Docker Compose
echo 4. Checking Docker Compose...
docker-compose --version
if %errorlevel% equ 0 (
    echo    [OK] Docker Compose is installed
) else (
    echo    [ERROR] Docker Compose is not installed
    exit /b 1
)
echo.

REM Verify project structure
echo 5. Verifying project structure...
set ALL_DIRS_EXIST=1

if exist "backend\src\main\java\com\codebaseqa\config" (echo    [OK] config) else (echo    [ERROR] config missing & set ALL_DIRS_EXIST=0)
if exist "backend\src\main\java\com\codebaseqa\controller" (echo    [OK] controller) else (echo    [ERROR] controller missing & set ALL_DIRS_EXIST=0)
if exist "backend\src\main\java\com\codebaseqa\service" (echo    [OK] service) else (echo    [ERROR] service missing & set ALL_DIRS_EXIST=0)
if exist "backend\src\main\java\com\codebaseqa\model" (echo    [OK] model) else (echo    [ERROR] model missing & set ALL_DIRS_EXIST=0)
if exist "backend\src\main\java\com\codebaseqa\repository" (echo    [OK] repository) else (echo    [ERROR] repository missing & set ALL_DIRS_EXIST=0)
echo.

REM Verify required files
echo 6. Verifying required files...
set ALL_FILES_EXIST=1

if exist "backend\pom.xml" (echo    [OK] pom.xml) else (echo    [ERROR] pom.xml missing & set ALL_FILES_EXIST=0)
if exist "backend\src\main\resources\application.yml" (echo    [OK] application.yml) else (echo    [ERROR] application.yml missing & set ALL_FILES_EXIST=0)
if exist "backend\src\main\java\com\codebaseqa\CodebaseQaApplication.java" (echo    [OK] CodebaseQaApplication.java) else (echo    [ERROR] CodebaseQaApplication.java missing & set ALL_FILES_EXIST=0)
if exist "docker-compose.yml" (echo    [OK] docker-compose.yml) else (echo    [ERROR] docker-compose.yml missing & set ALL_FILES_EXIST=0)
echo.

if %ALL_DIRS_EXIST% equ 0 (
    echo [ERROR] Some directories are missing
    exit /b 1
)

if %ALL_FILES_EXIST% equ 0 (
    echo [ERROR] Some files are missing
    exit /b 1
)

echo ==========================================
echo [OK] All Task 1.1 prerequisites verified!
echo ==========================================
echo.
echo Next steps:
echo 1. Run: docker-compose up -d
echo 2. Run: cd backend ^&^& mvn clean compile
echo 3. Run: cd backend ^&^& mvn spring-boot:run
