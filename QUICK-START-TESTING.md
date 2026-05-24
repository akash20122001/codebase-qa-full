# Quick Start: Testing Task 1.4

## 5-Minute Test Guide

### Step 1: Start Everything (2 minutes)

```bash
# Terminal 1: Start Docker containers
docker-compose up -d

# Terminal 2: Start backend
cd backend
./mvnw.cmd spring-boot:run

# Wait for: "Started CodebaseQaApplication in X seconds"
```

### Step 2: Verify Backend is Running (30 seconds)

```bash
# Should return: {"status":"UP"}
curl http://localhost:8080/actuator/health
```

### Step 3: Test Without Authentication (30 seconds)

```bash
# Should return: 403 Forbidden
curl http://localhost:8080/api/repos
```

✅ **Expected**: You get a 403 error because you're not authenticated. This is correct!

### Step 4: Get a JWT Token (2 minutes)

**Option A: Quick Manual Token (for testing)**

1. Create a test user:
```bash
docker exec -it codebaseqa-postgres psql -U postgres -d codebaseqa -c "
INSERT INTO users (id, github_id, username, email, avatar_url, github_token, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440000',
  12345678,
  'testuser',
  'test@example.com',
  'https://avatars.githubusercontent.com/u/12345678',
  'ghp_test_token',
  NOW(),
  NOW()
)
ON CONFLICT (id) DO NOTHING;
"
```

2. Generate JWT at https://jwt.io:
   - **Algorithm**: HS256
   - **Payload**: 
     ```json
     {
       "sub": "550e8400-e29b-41d4-a716-446655440000",
       "iat": 1516239022,
       "exp": 9999999999
     }
     ```
   - **Secret**: `your-secret-key-change-this-in-production-min-256-bits-long`
   - Copy the generated token

3. Save token to environment variable:
```bash
# Windows PowerShell
$JWT_TOKEN = "YOUR_GENERATED_TOKEN"

# Linux/Mac
export JWT_TOKEN="YOUR_GENERATED_TOKEN"
```

**Option B: Use GitHub OAuth (requires setup)**
- See TESTING-TASK-1.4.md for full OAuth setup instructions

### Step 5: Test Authenticated Endpoints (1 minute)

```bash
# Test 1: List repositories (should be empty)
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8080/api/repos

# Expected: {"data":[],"timestamp":"..."}
```

```bash
# Test 2: Connect a repository
curl -X POST \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"repoFullName": "octocat/Hello-World"}' \
  http://localhost:8080/api/repos

# Expected: 202 Accepted with repo details
```

```bash
# Test 3: List repositories again (should show 1)
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8080/api/repos

# Expected: {"data":[{...}],"timestamp":"..."}
```

---

## Expected Results

### ✅ Success Indicators

1. **Backend starts without errors**
2. **Health check returns UP**
3. **Unauthenticated requests return 403**
4. **Authenticated requests return 200/202**
5. **Repository is created in database**
6. **Indexing job is created**

### 🔍 Verify in Database

```bash
# Check repos table
docker exec -it codebaseqa-postgres psql -U postgres -d codebaseqa -c "
SELECT id, full_name, status, created_at FROM repos;
"

# Check indexing_jobs table
docker exec -it codebaseqa-postgres psql -U postgres -d codebaseqa -c "
SELECT id, status, job_type, created_at FROM indexing_jobs;
"
```

---

## Common Issues & Solutions

### Issue 1: "Connection refused" on port 8080
**Solution**: Backend isn't running. Start it with `./mvnw.cmd spring-boot:run`

### Issue 2: "Connection refused" on port 5432
**Solution**: PostgreSQL isn't running. Start it with `docker-compose up -d`

### Issue 3: "403 Forbidden" on all requests
**Solution**: You're not including the JWT token. Add `-H "Authorization: Bearer $JWT_TOKEN"`

### Issue 4: "401 Unauthorized"
**Solution**: Your JWT token is invalid or expired. Generate a new one.

### Issue 5: "Repository not found" when connecting
**Solution**: The repository name is wrong or doesn't exist. Try `octocat/Hello-World` (a public repo).

### Issue 6: "You don't have access to this repository"
**Solution**: Your GitHub token doesn't have access. Use a public repo or update the token.

### Issue 7: "Failed to send SQS message"
**Solution**: This is expected if SQS isn't configured. The repo is still created successfully.

---

## What to Look For

### In Terminal (Backend Logs)

```
✅ Good:
INFO  c.c.controller.RepoController : User testuser connecting repo: octocat/Hello-World
INFO  c.c.service.RepoService       : Repo connected and indexing queued: octocat/Hello-World (jobId=...)
INFO  c.c.service.SqsService        : Sent indexing message to SQS: jobId=..., repoId=...

⚠️ Expected Warning (if SQS not configured):
ERROR c.c.service.SqsService        : Failed to send SQS message
```

### In Database

```sql
-- Should see your repo
SELECT * FROM repos;

-- Should see a job with status 'QUEUED'
SELECT * FROM indexing_jobs;
```

---

## Next Steps

1. ✅ **Task 1.4 Complete**: Repository CRUD is working
2. ⏭️ **Task 1.5**: Build the frontend to interact with these APIs
3. ⏭️ **Task 2.1-2.4**: Implement the indexing pipeline

---

## Full Testing Resources

- **TESTING-TASK-1.4.md** - Comprehensive testing guide with all test cases
- **TASK-1.4-SUMMARY.md** - What we built and why
- **TASK-1.4-ARCHITECTURE.md** - System architecture diagrams
- **Task-1.4-Postman-Collection.json** - Import into Postman for easy testing

---

## Quick Commands Reference

```bash
# Start infrastructure
docker-compose up -d

# Start backend
cd backend && ./mvnw.cmd spring-boot:run

# Health check
curl http://localhost:8080/actuator/health

# List repos (authenticated)
curl -H "Authorization: Bearer $JWT_TOKEN" http://localhost:8080/api/repos

# Connect repo (authenticated)
curl -X POST -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"repoFullName": "octocat/Hello-World"}' \
  http://localhost:8080/api/repos

# Check database
docker exec -it codebaseqa-postgres psql -U postgres -d codebaseqa -c "SELECT * FROM repos;"

# Stop everything
docker-compose down
```

---

**Time to Complete**: ~5 minutes  
**Difficulty**: Easy  
**Prerequisites**: Docker, Java 21, Maven
