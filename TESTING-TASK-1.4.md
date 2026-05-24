# Testing Guide: Task 1.4 - Repository CRUD

## Overview

Task 1.4 implemented the Repository Management API that allows authenticated users to:
- Connect GitHub repositories for indexing
- List their connected repositories
- View repository details
- Disconnect repositories
- Trigger manual re-indexing

## Prerequisites

### 1. Infrastructure Running
```bash
docker-compose up -d
```

Verify:
```bash
docker ps
# Should show: codebaseqa-postgres and codebaseqa-redis
```

### 2. Backend Running
```bash
cd backend
./mvnw.cmd spring-boot:run
```

Verify:
```bash
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

### 3. Authentication Token

All repository endpoints require JWT authentication. You have two options:

#### Option A: Use GitHub OAuth (Recommended for Full Testing)
1. Create a GitHub OAuth App:
   - Go to https://github.com/settings/developers
   - Click "New OAuth App"
   - Application name: `Codebase QA Dev`
   - Homepage URL: `http://localhost:5173`
   - Authorization callback URL: `http://localhost:8080/api/auth/github/callback`
   - Click "Register application"
   - Copy the Client ID and generate a Client Secret

2. Update `backend/src/main/resources/application.yml`:
```yaml
app:
  github:
    client-id: YOUR_CLIENT_ID
    client-secret: YOUR_CLIENT_SECRET
```

3. Restart the backend

4. Get JWT token:
```bash
# Visit in browser:
http://localhost:8080/api/auth/github

# After authorizing, you'll be redirected with a JWT token
# Copy the token from the response
```

#### Option B: Manual JWT Generation (Quick Testing)

For quick testing without GitHub OAuth setup, you can manually generate a JWT:

1. First, create a test user in the database:
```bash
docker exec -it codebaseqa-postgres psql -U postgres -d codebaseqa -c "
INSERT INTO users (id, github_id, username, email, avatar_url, github_token, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440000',
  12345678,
  'testuser',
  'test@example.com',
  'https://avatars.githubusercontent.com/u/12345678',
  'ghp_YOUR_GITHUB_PERSONAL_ACCESS_TOKEN',
  NOW(),
  NOW()
)
ON CONFLICT (id) DO NOTHING;
"
```

2. Generate JWT using an online tool or write a small script:
   - Use https://jwt.io
   - Payload: `{"sub": "550e8400-e29b-41d4-a716-446655440000"}`
   - Secret: Your JWT secret from `application.yml` (default: `your-secret-key-change-this-in-production-min-256-bits-long`)
   - Algorithm: HS256

---

## Test Cases

### Test 1: List Repositories (Empty)

**Request:**
```bash
curl -X GET http://localhost:8080/api/repos \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response (200 OK):**
```json
{
  "data": [],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

### Test 2: Connect a Repository

**Important**: You need a valid GitHub Personal Access Token with `repo` scope for this to work.

**Request:**
```bash
curl -X POST http://localhost:8080/api/repos \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "repoFullName": "octocat/Hello-World",
    "branch": "master"
  }'
```

**Expected Response (202 Accepted):**
```json
{
  "data": {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "fullName": "octocat/Hello-World",
    "branch": "master",
    "status": "PENDING",
    "jobId": "770e8400-e29b-41d4-a716-446655440002",
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**What Happens Behind the Scenes:**
1. ✅ Backend verifies you have access to the repo on GitHub
2. ✅ Creates a `repos` record with status `PENDING`
3. ✅ Creates an `indexing_jobs` record with status `QUEUED`
4. ✅ Sends a message to SQS queue (will fail if SQS not configured - that's OK for now)
5. ✅ Returns 202 Accepted with job ID

---

### Test 3: List Repositories (After Connecting)

**Request:**
```bash
curl -X GET http://localhost:8080/api/repos \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response (200 OK):**
```json
{
  "data": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "fullName": "octocat/Hello-World",
      "branch": "master",
      "status": "PENDING",
      "totalChunks": 0,
      "lastIndexedAt": null,
      "webhookActive": false,
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

### Test 4: Get Specific Repository

**Request:**
```bash
curl -X GET http://localhost:8080/api/repos/660e8400-e29b-41d4-a716-446655440001 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response (200 OK):**
```json
{
  "data": {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "fullName": "octocat/Hello-World",
    "branch": "master",
    "status": "PENDING",
    "totalChunks": 0,
    "lastIndexedAt": null,
    "webhookActive": false,
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

### Test 5: Trigger Manual Re-index

**Request:**
```bash
curl -X POST http://localhost:8080/api/repos/660e8400-e29b-41d4-a716-446655440001/reindex \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response (202 Accepted):**
```json
{
  "data": {
    "jobId": "770e8400-e29b-41d4-a716-446655440003",
    "status": "QUEUED"
  },
  "timestamp": "2024-01-15T10:35:00Z"
}
```

---

### Test 6: Disconnect Repository

**Request:**
```bash
curl -X DELETE http://localhost:8080/api/repos/660e8400-e29b-41d4-a716-446655440001 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response (200 OK):**
```json
{
  "data": {
    "message": "Repository disconnected successfully"
  },
  "timestamp": "2024-01-15T10:40:00Z"
}
```

---

## Error Cases to Test

### Test 7: Invalid Repository Format

**Request:**
```bash
curl -X POST http://localhost:8080/api/repos \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "repoFullName": "invalid-format"
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Repository name must be in 'owner/repo' format"
  }
}
```

---

### Test 8: Repository Not Found on GitHub

**Request:**
```bash
curl -X POST http://localhost:8080/api/repos \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "repoFullName": "nonexistent/repository-that-does-not-exist"
  }'
```

**Expected Response (500 Internal Server Error):**
```json
{
  "error": {
    "message": "Repository not found"
  }
}
```

---

### Test 9: No Access to Repository

**Request:**
```bash
curl -X POST http://localhost:8080/api/repos \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "repoFullName": "private-org/private-repo"
  }'
```

**Expected Response (500 Internal Server Error):**
```json
{
  "error": {
    "message": "You don't have access to this repository"
  }
}
```

---

### Test 10: Duplicate Connection

**Request:** (Try to connect the same repo twice)
```bash
curl -X POST http://localhost:8080/api/repos \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "repoFullName": "octocat/Hello-World"
  }'
```

**Expected Response (500 Internal Server Error):**
```json
{
  "error": {
    "message": "Repository already connected"
  }
}
```

---

### Test 11: No Authentication

**Request:**
```bash
curl -X GET http://localhost:8080/api/repos \
  -H "Content-Type: application/json"
```

**Expected Response (403 Forbidden):**
```json
{
  "error": "Forbidden"
}
```

---

## Verify Database Changes

After connecting a repository, verify the database:

```bash
# Check repos table
docker exec -it codebaseqa-postgres psql -U postgres -d codebaseqa -c "
SELECT id, full_name, status, default_branch, created_at 
FROM repos;
"

# Check indexing_jobs table
docker exec -it codebaseqa-postgres psql -U postgres -d codebaseqa -c "
SELECT id, repo_id, status, job_type, created_at 
FROM indexing_jobs;
"
```

---

## Known Limitations (Expected)

1. **SQS Not Configured**: The SQS message sending will fail if you haven't configured AWS SQS. This is expected and won't break the flow. The repo and job are still created in the database.

2. **GitHub Token Required**: To actually connect a repository, you need a valid GitHub Personal Access Token with `repo` scope. The token should be stored in the `users.github_token` column.

3. **Indexing Won't Start**: The actual indexing won't happen until Task 2.4 (SQS Worker) is implemented. For now, repos will stay in `PENDING` status.

4. **Webhooks Not Created**: Webhook creation is implemented but not called during repo connection. This will be added in Task 3.2.

---

## Success Criteria Checklist

- ✅ `POST /api/repos` creates repo in DB with status PENDING
- ✅ `POST /api/repos` attempts to send message to SQS
- ✅ `GET /api/repos` returns list of user's repos
- ✅ `DELETE /api/repos/{id}` removes repo from DB
- ✅ Connecting a repo you don't have access to returns error
- ✅ All endpoints require JWT authentication
- ✅ Build compiles successfully

---

## Next Steps

After verifying Task 1.4 works:
1. **Task 1.5**: Build the frontend to interact with these APIs
2. **Task 2.1-2.3**: Implement the indexing pipeline
3. **Task 2.4**: Implement the SQS worker to process indexing jobs

---

## Troubleshooting

### Issue: "Failed to send SQS message"
**Solution**: This is expected if SQS is not configured. The repo is still created successfully. You can configure SQS later or use a local alternative like ElasticMQ.

### Issue: "Repository not found"
**Solution**: Make sure the repository name is correct and public, or that your GitHub token has access to it.

### Issue: "401 Unauthorized"
**Solution**: Make sure you're including the JWT token in the Authorization header: `Authorization: Bearer YOUR_TOKEN`

### Issue: "You don't have access to this repository"
**Solution**: The GitHub token in your user record doesn't have access to this repo. Use a repo you own or have access to.

---

## Quick Test Script

Save this as `quick-test.sh`:

```bash
#!/bin/bash

# Replace with your JWT token
JWT_TOKEN="YOUR_JWT_TOKEN_HERE"

echo "1. List repositories (should be empty)"
curl -s -X GET http://localhost:8080/api/repos \
  -H "Authorization: Bearer $JWT_TOKEN" | jq

echo -e "\n2. Connect a repository"
curl -s -X POST http://localhost:8080/api/repos \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"repoFullName": "octocat/Hello-World"}' | jq

echo -e "\n3. List repositories (should show 1)"
curl -s -X GET http://localhost:8080/api/repos \
  -H "Authorization: Bearer $JWT_TOKEN" | jq
```

Run with: `bash quick-test.sh`
