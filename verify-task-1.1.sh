#!/bin/bash

echo "=========================================="
echo "Task 1.1 Verification Script"
echo "=========================================="
echo ""

# Check Java version
echo "1. Checking Java version..."
java -version 2>&1 | head -n 1
if [ $? -eq 0 ]; then
    echo "   ✅ Java is installed"
else
    echo "   ❌ Java is not installed or not in PATH"
    exit 1
fi
echo ""

# Check Maven
echo "2. Checking Maven..."
mvn -version | head -n 1
if [ $? -eq 0 ]; then
    echo "   ✅ Maven is installed"
else
    echo "   ❌ Maven is not installed or not in PATH"
    exit 1
fi
echo ""

# Check Docker
echo "3. Checking Docker..."
docker --version
if [ $? -eq 0 ]; then
    echo "   ✅ Docker is installed"
else
    echo "   ❌ Docker is not installed or not in PATH"
    exit 1
fi
echo ""

# Check Docker Compose
echo "4. Checking Docker Compose..."
docker-compose --version
if [ $? -eq 0 ]; then
    echo "   ✅ Docker Compose is installed"
else
    echo "   ❌ Docker Compose is not installed"
    exit 1
fi
echo ""

# Verify project structure
echo "5. Verifying project structure..."
REQUIRED_DIRS=(
    "backend/src/main/java/com/codebaseqa/config"
    "backend/src/main/java/com/codebaseqa/controller"
    "backend/src/main/java/com/codebaseqa/service"
    "backend/src/main/java/com/codebaseqa/service/impl"
    "backend/src/main/java/com/codebaseqa/service/chunking"
    "backend/src/main/java/com/codebaseqa/service/prompt"
    "backend/src/main/java/com/codebaseqa/model"
    "backend/src/main/java/com/codebaseqa/repository"
    "backend/src/main/java/com/codebaseqa/dto/request"
    "backend/src/main/java/com/codebaseqa/dto/response"
    "backend/src/main/java/com/codebaseqa/exception"
    "backend/src/main/java/com/codebaseqa/util"
    "backend/src/main/java/com/codebaseqa/worker"
    "backend/src/main/java/com/codebaseqa/middleware"
)

ALL_DIRS_EXIST=true
for dir in "${REQUIRED_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        echo "   ✅ $dir"
    else
        echo "   ❌ $dir (missing)"
        ALL_DIRS_EXIST=false
    fi
done
echo ""

if [ "$ALL_DIRS_EXIST" = false ]; then
    echo "❌ Some directories are missing"
    exit 1
fi

# Verify required files
echo "6. Verifying required files..."
REQUIRED_FILES=(
    "backend/pom.xml"
    "backend/src/main/resources/application.yml"
    "backend/src/main/resources/application-dev.yml"
    "backend/src/main/resources/application-prod.yml"
    "backend/src/main/java/com/codebaseqa/CodebaseQaApplication.java"
    "docker-compose.yml"
    "backend/.env.example"
)

ALL_FILES_EXIST=true
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✅ $file"
    else
        echo "   ❌ $file (missing)"
        ALL_FILES_EXIST=false
    fi
done
echo ""

if [ "$ALL_FILES_EXIST" = false ]; then
    echo "❌ Some files are missing"
    exit 1
fi

echo "=========================================="
echo "✅ All Task 1.1 prerequisites verified!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Run: docker-compose up -d"
echo "2. Run: cd backend && mvn clean compile"
echo "3. Run: cd backend && mvn spring-boot:run"
