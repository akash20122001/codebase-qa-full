@echo off
echo ==========================================
echo Starting CodebaseQA Backend
echo ==========================================
echo.

REM Load environment variables from .env file
cd backend

REM AWS Configuration
set AWS_REGION=eu-north-1
set AWS_ACCESS_KEY_ID=your-aws-access-key-id
set AWS_SECRET_ACCESS_KEY=your-aws-secret-access-key
set SQS_QUEUE_URL=https://sqs.eu-north-1.amazonaws.com/YOUR-AWS-ACCOUNT-ID/codebaseqa-indexing-queue

REM GitHub OAuth
set GITHUB_CLIENT_ID=your-github-client-id
set GITHUB_CLIENT_SECRET=your-github-client-secret

REM JWT
set JWT_SECRET=your-super-secret-key-minimum-32-characters-long-change-this-in-production

echo Environment variables set!
echo.
echo Starting Spring Boot application...
echo.

call mvnw.cmd spring-boot:run

pause
