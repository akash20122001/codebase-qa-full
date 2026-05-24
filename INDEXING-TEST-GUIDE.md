# Indexing Flow Testing Guide

## Prerequisites Checklist

Before testing, ensure these services are running:

### 1. PostgreSQL with pgvector
```bash
# Check if running
docker ps | grep postgres

# If not running, start it
docker-compose up -d postgres

# Verify pgvector extension
docker exec -it <postgres-container> psql -U postgres -d codebaseqa -c "SELECT * FROM pg_extension WHERE extname = 'vector';"
```

### 2. Redis
```bash
# Check if running
docker ps | grep redis

# If not running
docker-compose up -d redis

# Test connection
redis-cli ping
# Should return: PONG
```

### 3. AWS SQS
Your SQS queue is already configured:
- Queue URL: `https://sqs.eu-north-1.amazonaws.com/019358088406/codebaseqa-indexing-queue`
- Region: `eu-north-1`

Verify access:
```bash
aws sqs get-queue-attributes --queue-url https://sqs.eu-north-1.amazonaws.com/019358088406/codebaseqa-indexing-queue --attribute-names All --region eu-north-1
```

---

## Testing Methods

You have **3 ways** to test the indexing flow:

---

## **Method 1: Full End-to-End Test (Recommended)**

This tests the complete flow: API → SQS → Worker → Database

### Step 1: Start the Backend

```bash
cd backend
./mvnw.cmd spring-boot:run
```

**Wait for:**
```
Started CodebaseQaApplication in X.XXX seconds
```

### Step 2: Authenticate with GitHub

**Option A: Using Browser**
1. Open: http://localhost:8080/api/auth/github
2. Authorize the app
3. Copy the JWT token from the response

**Option B: Using curl**
```bash
# This will redirect you to GitHub
curl -L http://localhost:8080/api/auth/github
```

### Step 3: Connect a Repository

**Choose a small test repository** (important for first test):
- ✅ Good: `octocat/Hello-World` (small, public)
- ✅ Good: `spring-projects/spring-petclinic` (medium)
- ❌ Bad: `kubernetes/kubernetes` (huge, will take forever)

**API Request:**
```bash
curl -X POST http://localhost:8080/api/repos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "fullName": "octocat/Hello-World",
    "defaultBranch": "master"
  }'
```

**Expected Response:**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "fullName": "octocat/Hello-World",
    "branch": "master",
    "status": "PENDING"
  }
}
```

### Step 4: Monitor the Indexing Process

**Watch the logs:**
```bash
# In the terminal where backend is running, you'll see:

[INFO] Processing indexing job: jobId=xxx, repoId=xxx
[INFO] Cloning repo: octocat/Hello-World
[INFO] Found 5 files to index in octocat/Hello-World
[INFO] Parsed 5 files into 12 chunks for octocat/Hello-World
[INFO] Embedding 12 chunks for octocat/Hello-World
[INFO] Saving 12 chunks to database
[INFO] ✅ Indexing completed for octocat/Hello-World: 12 chunks
[INFO] ✅ Indexing job completed successfully: xxx
```

**Check job status:**
```bash
# Get repo details
curl http://localhost:8080/api/repos \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "fullName": "octocat/Hello-World",
      "status": "READY",  // ← Changed from PENDING
      "totalChunks": 12,
      "lastIndexedAt": "2026-05-18T16:30:00Z"
    }
  ]
}
```

### Step 5: Verify in Database

**Connect to PostgreSQL:**
```bash
docker exec -it <postgres-container> psql -U postgres -d codebaseqa
```

**Check the data:**
```sql
-- Check repos
SELECT id, full_name, status, total_chunks FROM repos;

-- Check indexing jobs
SELECT id, status, total_files, processed_files, progress FROM indexing_jobs;

-- Check code chunks
SELECT id, file_path, chunk_type, chunk_name, start_line, end_line 
FROM code_chunks 
LIMIT 10;

-- Check embeddings (should see vector data)
SELECT id, file_path, LEFT(embedding, 50) as embedding_preview 
FROM code_chunks 
LIMIT 5;

-- Count chunks per repo
SELECT r.full_name, COUNT(c.id) as chunk_count
FROM repos r
LEFT JOIN code_chunks c ON c.repo_id = r.id
GROUP BY r.full_name;
```

---

## **Method 2: Direct Service Test (Skip SQS)**

Test the indexing service directly without the worker/queue.

### Step 1: Create a Test Controller

Create `backend/src/main/java/com/codebaseqa/controller/TestController.java`:

```java
package com.codebaseqa.controller;

import com.codebaseqa.model.IndexingJob;
import com.codebaseqa.model.Repo;
import com.codebaseqa.repository.IndexingJobRepository;
import com.codebaseqa.repository.RepoRepository;
import com.codebaseqa.service.IndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final IndexingService indexingService;
    private final RepoRepository repoRepository;
    private final IndexingJobRepository jobRepository;

    @PostMapping("/index/{repoId}")
    public ResponseEntity<Map<String, Object>> testIndexing(@PathVariable UUID repoId) {
        try {
            Repo repo = repoRepository.findById(repoId).orElseThrow();
            
            // Create a test job
            IndexingJob job = IndexingJob.builder()
                .repo(repo)
                .jobType(IndexingJob.JobType.FULL)
                .status(IndexingJob.JobStatus.QUEUED)
                .build();
            job = jobRepository.save(job);
            
            // Call indexing directly
            indexingService.processFullIndexing(job.getId(), repoId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Indexing completed",
                "jobId", job.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}
```

### Step 2: Test It

```bash
# First, connect a repo using Method 1 Step 3
# Then call the test endpoint

curl -X POST http://localhost:8080/api/test/index/{REPO_ID} \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

This bypasses SQS and calls `IndexingService` directly.

---

## **Method 3: Unit Test (Fastest)**

Test individual components in isolation.

### Create Test File

Create `backend/src/test/java/com/codebaseqa/service/IndexingServiceTest.java`:

```java
package com.codebaseqa.service;

import com.codebaseqa.model.Repo;
import com.codebaseqa.model.User;
import com.codebaseqa.model.IndexingJob;
import com.codebaseqa.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IndexingServiceTest {

    @Autowired
    private IndexingService indexingService;
    
    @Autowired
    private RepoRepository repoRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private IndexingJobRepository jobRepository;
    
    @Autowired
    private ChunkRepository chunkRepository;

    @Test
    void testFullIndexing() {
        // 1. Create test user
        User user = User.builder()
            .username("testuser")
            .githubId(12345L)
            .githubToken("fake-token-for-public-repo")
            .build();
        user = userRepository.save(user);

        // 2. Create test repo (use a small public repo)
        Repo repo = Repo.builder()
            .user(user)
            .fullName("octocat/Hello-World")
            .defaultBranch("master")
            .status(Repo.RepoStatus.PENDING)
            .build();
        repo = repoRepository.save(repo);

        // 3. Create job
        IndexingJob job = IndexingJob.builder()
            .repo(repo)
            .jobType(IndexingJob.JobType.FULL)
            .status(IndexingJob.JobStatus.QUEUED)
            .build();
        job = jobRepository.save(job);

        // 4. Run indexing
        indexingService.processFullIndexing(job.getId(), repo.getId());

        // 5. Verify results
        job = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(IndexingJob.JobStatus.COMPLETED, job.getStatus());
        
        repo = repoRepository.findById(repo.getId()).orElseThrow();
        assertEquals(Repo.RepoStatus.READY, repo.getStatus());
        assertTrue(repo.getTotalChunks() > 0);
        
        long chunkCount = chunkRepository.countByRepoId(repo.getId());
        assertTrue(chunkCount > 0);
        
        System.out.println("✅ Indexed " + chunkCount + " chunks");
    }
}
```

### Run the Test

```bash
cd backend
./mvnw.cmd test -Dtest=IndexingServiceTest
```

---

## **Troubleshooting**

### Issue 1: "Job not found"
**Cause:** Worker processed job before you could check status.
**Solution:** Use a larger repo or add a breakpoint in `IndexingService`.

### Issue 2: "Failed to clone repository"
**Cause:** Invalid GitHub token or private repo without access.
**Solution:** 
- Use a public repo for testing
- Ensure your GitHub token has `repo` scope

### Issue 3: "Circuit breaker activated"
**Cause:** Gemini API key invalid or rate limited.
**Solution:**
```bash
# Test Gemini API directly
curl "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=YOUR_KEY" \
  -H "Content-Type: application/json" \
  -d '{"content":{"parts":[{"text":"test"}]}}'
```

### Issue 4: "SQS queue not found"
**Cause:** Queue URL incorrect or AWS credentials invalid.
**Solution:**
```bash
# Test SQS access
aws sqs list-queues --region eu-north-1
```

### Issue 5: Worker not polling
**Cause:** `@EnableScheduling` not enabled or SQS URL empty.
**Solution:** Check logs for:
```
SQS queue URL not configured, skipping poll
```

---

## **Expected Timeline**

For `octocat/Hello-World` (small repo):
- Clone: ~5 seconds
- Parse & Chunk: ~2 seconds
- Embed: ~10 seconds (12 chunks × ~1 sec each)
- Save to DB: ~1 second
- **Total: ~20 seconds**

For `spring-projects/spring-petclinic` (medium repo):
- Clone: ~15 seconds
- Parse & Chunk: ~10 seconds
- Embed: ~2 minutes (200 chunks)
- Save to DB: ~5 seconds
- **Total: ~2.5 minutes**

---

## **Success Criteria**

✅ **Indexing is successful if:**

1. **Job status** changes: `QUEUED` → `PROCESSING` → `COMPLETED`
2. **Repo status** changes: `PENDING` → `INDEXING` → `READY`
3. **Database has chunks:**
   ```sql
   SELECT COUNT(*) FROM code_chunks WHERE repo_id = 'YOUR_REPO_ID';
   -- Should return > 0
   ```
4. **Embeddings are present:**
   ```sql
   SELECT embedding FROM code_chunks LIMIT 1;
   -- Should return: [0.123,0.456,...]
   ```
5. **Logs show success:**
   ```
   ✅ Indexing completed for {repo}: {N} chunks
   ```

---

## **Next Steps After Successful Test**

Once indexing works, you can:

1. **Test with your own repo** (if private, ensure token has access)
2. **Test incremental indexing** (modify a file, trigger webhook)
3. **Move to Task 2.5** (Query Service - ask questions about the indexed code)

---

## **Quick Test Script**

Save this as `test-indexing.sh`:

```bash
#!/bin/bash

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo "🧪 Testing Indexing Flow..."

# 1. Check services
echo "1️⃣ Checking PostgreSQL..."
docker ps | grep postgres > /dev/null && echo -e "${GREEN}✓ PostgreSQL running${NC}" || echo -e "${RED}✗ PostgreSQL not running${NC}"

echo "2️⃣ Checking Redis..."
docker ps | grep redis > /dev/null && echo -e "${GREEN}✓ Redis running${NC}" || echo -e "${RED}✗ Redis not running${NC}"

echo "3️⃣ Checking Backend..."
curl -s http://localhost:8080/actuator/health > /dev/null && echo -e "${GREEN}✓ Backend running${NC}" || echo -e "${RED}✗ Backend not running${NC}"

# 2. Authenticate
echo "4️⃣ Authenticating..."
echo "Open: http://localhost:8080/api/auth/github"
echo "Paste your JWT token:"
read JWT_TOKEN

# 3. Connect repo
echo "5️⃣ Connecting test repo..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/repos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"fullName":"octocat/Hello-World","defaultBranch":"master"}')

REPO_ID=$(echo $RESPONSE | jq -r '.data.id')
echo "Repo ID: $REPO_ID"

# 4. Wait and check status
echo "6️⃣ Waiting for indexing to complete..."
for i in {1..30}; do
  sleep 2
  STATUS=$(curl -s http://localhost:8080/api/repos \
    -H "Authorization: Bearer $JWT_TOKEN" | jq -r ".data[] | select(.id==\"$REPO_ID\") | .status")
  
  echo "Status: $STATUS"
  
  if [ "$STATUS" == "READY" ]; then
    echo -e "${GREEN}✅ Indexing completed!${NC}"
    break
  elif [ "$STATUS" == "FAILED" ]; then
    echo -e "${RED}❌ Indexing failed!${NC}"
    break
  fi
done

# 5. Verify chunks
echo "7️⃣ Checking database..."
docker exec -it $(docker ps | grep postgres | awk '{print $1}') \
  psql -U postgres -d codebaseqa -c "SELECT COUNT(*) FROM code_chunks WHERE repo_id = '$REPO_ID';"

echo -e "${GREEN}✅ Test complete!${NC}"
```

Run it:
```bash
chmod +x test-indexing.sh
./test-indexing.sh
```

---

## **Recommended Test Order**

1. ✅ **Start here:** Method 1 with `octocat/Hello-World`
2. ✅ **Then:** Method 1 with your own small repo
3. ✅ **Finally:** Method 3 (unit tests) for CI/CD

Good luck! 🚀
