# Postman Testing Guide for CodebaseQA API

## Overview

This guide will help you test the CodebaseQA Query Service (Task 2.5) using Postman. The collection includes all endpoints for authentication, repository management, and the RAG query pipeline.

---

## 📦 Files Included

1. **CodebaseQA-API.postman_collection.json** - Complete API collection with all endpoints
2. **CodebaseQA-Local.postman_environment.json** - Environment variables for local testing

---

## 🚀 Quick Start

### Step 1: Import Collection and Environment

1. Open Postman
2. Click **Import** button (top left)
3. Drag and drop both JSON files:
   - `CodebaseQA-API.postman_collection.json`
   - `CodebaseQA-Local.postman_environment.json`
4. Click **Import**

### Step 2: Select Environment

1. In the top-right corner, select **CodebaseQA - Local** from the environment dropdown
2. Click the eye icon (👁️) to view/edit environment variables

### Step 3: Start Backend

Make sure your backend is running:

```bash
cd backend
./mvnw.cmd spring-boot:run
```

Or if you have Maven installed:

```bash
cd backend
mvn spring-boot:run
```

### Step 4: Verify Backend is Running

1. In Postman, open the **Health Check** folder
2. Send the **Health Check** request
3. You should get a `200 OK` response with status `UP`

---

## 🔐 Authentication Setup

### Option 1: Get JWT via Browser (Recommended)

1. In Postman, go to **Authentication** folder
2. Copy the URL from **GitHub OAuth (Browser)** request
3. Open that URL in your browser: `http://localhost:8080/api/auth/github`
4. Authorize with GitHub
5. After redirect, you'll see a JSON response with a `token` field
6. Copy the token value
7. In Postman:
   - Click the environment dropdown (top-right)
   - Click the eye icon (👁️)
   - Click **Edit** next to "CodebaseQA - Local"
   - Paste the token into the `jwt_token` value field
   - Click **Save**

### Option 2: Use Existing Token

If you already have a JWT token from the frontend or previous session:

1. Click the environment dropdown (top-right)
2. Click the eye icon (👁️)
3. Click **Edit**
4. Paste your token into `jwt_token`
5. Click **Save**

### Verify Authentication

1. Send the **Get Current User** request
2. You should get your user profile (username, email, etc.)
3. If you get `401 Unauthorized`, your token is invalid or expired

---

## 📋 Testing Workflow

### 1. List Repositories

**Purpose:** Get a repository ID for queries

1. Go to **Repositories** folder
2. Send **List Repositories** request
3. Copy a `repoId` from the response (must have `status: "READY"`)
4. Update environment variable:
   - Click environment dropdown → Edit
   - Paste the ID into `repo_id`
   - Save

**Example Response:**
```json
{
  "data": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "fullName": "octocat/hello-world",
      "status": "READY",
      "totalChunks": 245
    }
  ]
}
```

### 2. Ask Your First Question

**Purpose:** Test the RAG pipeline with a new conversation

1. Go to **Query (RAG Pipeline)** folder
2. Open **Ask Question (New Conversation)** request
3. Edit the request body if desired:
   ```json
   {
     "repoId": "{{repo_id}}",
     "question": "How does the authentication work?"
   }
   ```
4. Click **Send**

**Understanding the Response:**

Postman will show the raw SSE (Server-Sent Events) stream:

```
event: citations
data: [{"filePath":"src/auth/JwtService.java","startLine":15,"endLine":42,...}]

event: token
data: {"content":"The"}

event: token
data: {"content":" authentication"}

event: token
data: {"content":" system"}

...

event: done
data: {"messageId":"990e...","conversationId":"880e...","tokenCount":342}
```

**Important:** Save the `conversationId` from the `done` event for follow-up questions!

### 3. Ask a Follow-up Question

**Purpose:** Test conversation context

1. Copy the `conversationId` from the previous response's `done` event
2. Update environment variable:
   - Click environment dropdown → Edit
   - Paste into `conversation_id`
   - Save
3. Send **Ask Follow-up Question** request
4. The LLM should reference previous conversation context

### 4. Test Caching

**Purpose:** Verify cached responses are faster

1. Send the same question twice (exact same text)
2. First request: ~2-5 seconds (calls LLM)
3. Second request: <100ms (cached)
4. Check backend logs for "Cache hit for query on repo"

### 5. Test Rate Limiting

**Purpose:** Verify rate limiting works (20 queries/hour)

1. Open **Test Rate Limiting** request
2. Use Postman's **Collection Runner**:
   - Click **Collections** tab
   - Right-click on "Query (RAG Pipeline)" folder
   - Select **Run folder**
   - Set iterations to 21
   - Click **Run**
3. First 20 should succeed
4. 21st should return error event:
   ```
   event: error
   data: {"code":"RATE_LIMIT_EXCEEDED","message":"...","retryAfter":3600}
   ```

**Reset Rate Limit (for testing):**
```bash
# Connect to Redis
redis-cli

# Delete rate limit key
DEL ratelimit:query:<your-user-id>
```

### 6. Test Error Scenarios

#### A. Query Non-Indexed Repository

1. Connect a new repository (it will be PENDING)
2. Copy its ID to `pending_repo_id` environment variable
3. Send **Query Non-Indexed Repo (Error Test)** request
4. Expected: Error event with `REPO_NOT_INDEXED`

#### B. Query with Long Question

1. Send **Query with Long Question (Validation Test)** request
2. Expected: HTTP 400 Bad Request with validation error

#### C. Query Without Authentication

1. Remove the Authorization header
2. Send any query request
3. Expected: HTTP 401 Unauthorized

---

## 🎯 Test Scenarios Checklist

Use this checklist to verify all functionality:

### Basic Functionality
- [ ] Health check returns 200 OK
- [ ] Get current user returns profile
- [ ] List repositories returns repos
- [ ] Ask question returns SSE stream with citations, tokens, and done event
- [ ] Follow-up question uses conversation context
- [ ] Second identical query is cached (faster response)

### Rate Limiting
- [ ] 20 queries succeed
- [ ] 21st query returns RATE_LIMIT_EXCEEDED error
- [ ] Rate limit resets after 1 hour

### Error Handling
- [ ] Query on non-indexed repo returns REPO_NOT_INDEXED
- [ ] Query with >1000 char question returns 400 validation error
- [ ] Query without JWT returns 401 Unauthorized
- [ ] Query on another user's repo returns 403 Forbidden

### Response Quality
- [ ] Citations include relevant code files
- [ ] Line numbers in citations are accurate
- [ ] LLM response references the cited code
- [ ] Follow-up questions maintain context

---

## 🔍 Debugging Tips

### View SSE Stream Better

Postman shows raw SSE, which can be hard to read. For better visualization:

**Option 1: Use Postman Visualize**

Add this to the **Tests** tab of a query request:

```javascript
// Parse SSE stream
const responseText = pm.response.text();
const events = [];
let currentEvent = null;

responseText.split('\n').forEach(line => {
    if (line.startsWith('event: ')) {
        currentEvent = line.substring(7).trim();
    } else if (line.startsWith('data: ')) {
        const data = line.substring(6);
        try {
            events.push({
                event: currentEvent,
                data: JSON.parse(data)
            });
        } catch (e) {
            events.push({
                event: currentEvent,
                data: data
            });
        }
    }
});

// Create HTML visualization
const template = `
<h2>SSE Events</h2>
<div>
    {{#each events}}
        <div style="margin: 10px 0; padding: 10px; border: 1px solid #ccc; border-radius: 5px;">
            <strong>Event: {{event}}</strong>
            <pre>{{json data}}</pre>
        </div>
    {{/each}}
</div>
`;

pm.visualizer.set(template, { events });
```

Then click the **Visualize** tab after sending the request.

**Option 2: Use Browser DevTools**

```javascript
// Run this in browser console
const eventSource = new EventSource('http://localhost:8080/api/query', {
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN'
  }
});

eventSource.addEventListener('citations', (e) => {
  console.log('Citations:', JSON.parse(e.data));
});

eventSource.addEventListener('token', (e) => {
  console.log('Token:', JSON.parse(e.data));
});

eventSource.addEventListener('done', (e) => {
  console.log('Done:', JSON.parse(e.data));
  eventSource.close();
});

eventSource.addEventListener('error', (e) => {
  console.error('Error:', JSON.parse(e.data));
  eventSource.close();
});
```

**Note:** EventSource doesn't support POST, so you'll need to use fetch with ReadableStream (see QUERY-API-USAGE.md).

### Check Backend Logs

Enable debug logging in `application.yml`:

```yaml
logging:
  level:
    com.codebaseqa.service.QueryService: DEBUG
    com.codebaseqa.service.impl.GeminiLlmService: DEBUG
```

Look for:
- "Cache hit for query on repo" - Caching working
- "Rate limit exceeded for user" - Rate limiting triggered
- Circuit breaker state changes - Fault tolerance

### Check Database

```sql
-- Recent conversations
SELECT * FROM conversations ORDER BY created_at DESC LIMIT 10;

-- Recent messages
SELECT * FROM messages ORDER BY created_at DESC LIMIT 20;

-- Check citations
SELECT id, role, citations FROM messages WHERE citations IS NOT NULL LIMIT 5;
```

### Check Redis Cache

```bash
redis-cli

# View all query cache keys
KEYS "query:*"

# View rate limit keys
KEYS "ratelimit:*"

# Get specific cache entry
GET "query:<repo-id>:<hash>"

# Check rate limit count
GET "ratelimit:query:<user-id>"

# Check TTL
TTL "ratelimit:query:<user-id>"
```

---

## 🐛 Common Issues

### Issue: "Cannot read property 'id' of undefined"

**Cause:** `repo_id` environment variable not set

**Solution:**
1. Send **List Repositories** request
2. Copy a repo ID with status "READY"
3. Update `repo_id` in environment variables

### Issue: "401 Unauthorized"

**Cause:** JWT token missing or expired

**Solution:**
1. Get a new token via GitHub OAuth
2. Update `jwt_token` in environment variables

### Issue: "REPO_NOT_INDEXED"

**Cause:** Repository status is not READY

**Solution:**
1. Check repo status with **Get Repository Details**
2. Wait for indexing to complete
3. Status should be READY before querying

### Issue: "Rate limit exceeded" immediately

**Cause:** Previous test left rate limit active

**Solution:**
```bash
redis-cli DEL "ratelimit:query:<your-user-id>"
```

### Issue: No response or timeout

**Cause:** Backend not running or Gemini API issue

**Solution:**
1. Check backend is running: `curl http://localhost:8080/actuator/health`
2. Check Gemini API key in `.env` or `application.yml`
3. Check circuit breaker status in health endpoint

### Issue: SSE stream is hard to read

**Cause:** Postman shows raw SSE format

**Solution:**
- Use the Visualize script above
- Or test with a custom client (see QUERY-API-USAGE.md)
- Or use browser DevTools

---

## 📊 Performance Benchmarks

Expected response times:

| Scenario | Expected Time |
|----------|---------------|
| Health check | <50ms |
| List repositories | <100ms |
| First query (no cache) | 2-5 seconds |
| Cached query | <100ms |
| Rate limit check | <10ms |
| Vector search | <100ms |

If responses are slower:
- Check database query performance
- Verify Redis is running
- Check Gemini API latency
- Review backend logs for bottlenecks

---

## 🎓 Advanced Testing

### Load Testing with Collection Runner

1. Right-click on "Query (RAG Pipeline)" folder
2. Select **Run folder**
3. Set iterations (e.g., 10)
4. Set delay between requests (e.g., 1000ms)
5. Click **Run**
6. Review results for failures

### Automated Testing with Newman

Install Newman (Postman CLI):

```bash
npm install -g newman
```

Run collection:

```bash
newman run CodebaseQA-API.postman_collection.json \
  -e CodebaseQA-Local.postman_environment.json \
  --reporters cli,json \
  --reporter-json-export results.json
```

### CI/CD Integration

Add to your CI pipeline:

```yaml
# .github/workflows/api-tests.yml
name: API Tests
on: [push]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Start backend
        run: |
          cd backend
          ./mvnw spring-boot:run &
          sleep 30
      - name: Run Postman tests
        run: |
          npm install -g newman
          newman run backend/CodebaseQA-API.postman_collection.json \
            -e backend/CodebaseQA-Local.postman_environment.json
```

---

## 📝 Next Steps

After testing Task 2.5:

1. **Task 2.6:** Test Conversation Service endpoints (when implemented)
2. **Task 2.7:** Test with frontend Chat UI
3. **Task 3.2:** Test webhook integration

---

## 🆘 Getting Help

If you encounter issues:

1. Check backend logs
2. Verify environment variables are set
3. Check database and Redis are running
4. Review the TASK-2.5-TEST-PLAN.md for detailed test scenarios
5. Check QUERY-API-USAGE.md for API reference

---

## 📚 Additional Resources

- **API Specification:** `04-api-specification.md`
- **Implementation Details:** `TASK-2.5-IMPLEMENTATION.md`
- **Test Plan:** `TASK-2.5-TEST-PLAN.md`
- **API Usage Examples:** `QUERY-API-USAGE.md`
