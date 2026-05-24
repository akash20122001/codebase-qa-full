# Postman Testing Files

## 📦 What's Included

This directory contains everything you need to test the CodebaseQA API using Postman.

### Files

1. **CodebaseQA-API.postman_collection.json**
   - Complete API collection with 11 requests
   - Organized into folders: Authentication, Repositories, Query (RAG Pipeline), Health Check
   - Includes test scenarios for happy path, errors, and edge cases

2. **CodebaseQA-Local.postman_environment.json**
   - Environment variables for local development
   - Pre-configured with localhost:8080
   - Variables: base_url, jwt_token, repo_id, conversation_id, pending_repo_id

3. **POSTMAN-TESTING-GUIDE.md**
   - Comprehensive guide (3000+ words)
   - Step-by-step setup instructions
   - Detailed test scenarios
   - Debugging tips and troubleshooting
   - Advanced testing techniques

4. **POSTMAN-QUICK-REFERENCE.md**
   - Quick reference card
   - 5-minute setup guide
   - Essential requests table
   - Common troubleshooting
   - Test checklists

---

## 🚀 Quick Start

### 1. Import into Postman

**Option A: Drag & Drop**
1. Open Postman
2. Click **Import** button
3. Drag both JSON files into the import window
4. Click **Import**

**Option B: File Import**
1. Open Postman
2. Click **Import** → **Files**
3. Select both JSON files
4. Click **Open**

### 2. Select Environment

1. Top-right corner: Select **CodebaseQA - Local** from dropdown
2. Click eye icon (👁️) to view variables

### 3. Start Testing

1. Start backend: `./mvnw.cmd spring-boot:run`
2. Get JWT token via GitHub OAuth
3. Update `jwt_token` in environment
4. Send requests!

---

## 📋 Collection Structure

```
CodebaseQA API - Task 2.5
├── Authentication
│   ├── Get Current User
│   └── GitHub OAuth (Browser)
├── Repositories
│   ├── List Repositories
│   ├── Get Repository Details
│   └── Connect Repository
├── Query (RAG Pipeline)
│   ├── Ask Question (New Conversation)
│   ├── Ask Follow-up Question
│   ├── Test Rate Limiting
│   ├── Query Non-Indexed Repo (Error Test)
│   └── Query with Long Question (Validation Test)
└── Health Check
    └── Health Check
```

---

## 🎯 What You Can Test

### ✅ Core Functionality
- RAG pipeline with vector search
- LLM streaming responses (SSE)
- Conversation management
- Code citations with line numbers
- Query caching

### ✅ Security & Limits
- JWT authentication
- Rate limiting (20 queries/hour)
- Repository access control
- Input validation

### ✅ Error Handling
- Non-indexed repositories
- Invalid requests
- Rate limit exceeded
- Circuit breaker behavior

### ✅ Performance
- Response times
- Cache effectiveness
- Concurrent requests
- Load testing

---

## 📚 Documentation

| Document | Purpose | When to Use |
|----------|---------|-------------|
| **POSTMAN-QUICK-REFERENCE.md** | Quick setup & common tasks | First time setup, quick reference |
| **POSTMAN-TESTING-GUIDE.md** | Comprehensive guide | Detailed testing, troubleshooting |
| **TASK-2.5-TEST-PLAN.md** | Test scenarios & acceptance criteria | Systematic testing, QA |
| **QUERY-API-USAGE.md** | API reference & code examples | Integration, client development |
| **04-api-specification.md** | Full API specification | API design, contract validation |

---

## 🔑 Environment Variables Explained

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `base_url` | Yes | Backend URL | `http://localhost:8080` |
| `jwt_token` | Yes | Authentication token | `eyJhbGciOiJIUzI1NiIs...` |
| `repo_id` | Yes | Repository to query | `660e8400-e29b-41d4-a716-446655440001` |
| `conversation_id` | No | For follow-up questions | `880e8400-e29b-41d4-a716-446655440010` |
| `pending_repo_id` | No | For error testing | `770e8400-e29b-41d4-a716-446655440002` |

### How to Get Values

**jwt_token:**
1. Open in browser: `http://localhost:8080/api/auth/github`
2. Authorize with GitHub
3. Copy token from response

**repo_id:**
1. Send "List Repositories" request
2. Copy ID of a repo with status "READY"

**conversation_id:**
1. Send "Ask Question" request
2. Copy conversationId from the `done` event in response

**pending_repo_id:**
1. Connect a new repository
2. Copy its ID (status will be PENDING)

---

## 🎓 Testing Workflows

### Workflow 1: First-Time Setup (5 min)
```
1. Import collection & environment
2. Start backend
3. Get JWT token via browser
4. Update jwt_token in environment
5. Send "Health Check" → Verify 200 OK
6. Send "Get Current User" → Verify authentication
7. Send "List Repositories" → Get repo_id
8. Update repo_id in environment
9. Send "Ask Question" → Test RAG pipeline
```

### Workflow 2: Full Test Suite (15 min)
```
1. Basic functionality (5 requests)
2. Error scenarios (3 requests)
3. Rate limiting (21 requests)
4. Performance testing (cache test)
5. Verify database state
6. Check Redis cache
```

### Workflow 3: Regression Testing (10 min)
```
1. Health check
2. Authentication
3. One query (happy path)
4. One error scenario
5. Cache test
```

---

## 🐛 Common Issues & Solutions

### "Cannot import collection"
- **Cause:** Invalid JSON
- **Solution:** Re-download files, ensure no corruption

### "401 Unauthorized"
- **Cause:** Missing or expired JWT token
- **Solution:** Get new token via GitHub OAuth

### "Variable not found: repo_id"
- **Cause:** Environment variable not set
- **Solution:** Send "List Repositories", copy ID, update environment

### "REPO_NOT_INDEXED"
- **Cause:** Repository not ready
- **Solution:** Wait for indexing, check status with "Get Repository Details"

### "Connection refused"
- **Cause:** Backend not running
- **Solution:** Start backend with `./mvnw.cmd spring-boot:run`

---

## 📊 Expected Results

### Health Check
```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP"
    }
  }
}
```

### List Repositories
```json
{
  "data": [
    {
      "id": "660e8400-...",
      "fullName": "octocat/hello-world",
      "status": "READY",
      "totalChunks": 245
    }
  ]
}
```

### Ask Question (SSE Stream)
```
event: citations
data: [{"filePath":"src/auth/...","startLine":15,...}]

event: token
data: {"content":"The"}

event: token
data: {"content":" authentication"}

...

event: done
data: {"messageId":"...","conversationId":"...","tokenCount":342}
```

---

## 🔧 Advanced Features

### Collection Runner
- Run multiple requests in sequence
- Test rate limiting (21 iterations)
- Load testing with delays
- Export results to JSON

### Newman (CLI)
```bash
npm install -g newman
newman run CodebaseQA-API.postman_collection.json \
  -e CodebaseQA-Local.postman_environment.json
```

### Pre-request Scripts
- Dynamic variable generation
- Token refresh logic
- Timestamp generation

### Test Scripts
- Response validation
- Assertion testing
- Variable extraction
- Automated workflows

---

## 📈 Performance Benchmarks

| Request | Expected Time | Notes |
|---------|---------------|-------|
| Health Check | <50ms | Simple status check |
| List Repositories | <100ms | Database query |
| Ask Question (first) | 2-5s | LLM call + vector search |
| Ask Question (cached) | <100ms | Redis cache hit |
| Rate Limit Check | <10ms | Redis operation |

---

## 🎯 Test Coverage

This collection covers:

- ✅ All Task 2.5 endpoints
- ✅ Authentication flow
- ✅ Repository management
- ✅ RAG query pipeline
- ✅ SSE streaming
- ✅ Rate limiting
- ✅ Caching
- ✅ Error handling
- ✅ Input validation
- ✅ Circuit breaker

**Not covered (future tasks):**
- ❌ Conversation CRUD (Task 2.6)
- ❌ Webhook integration (Task 3.2)
- ❌ Frontend integration (Task 2.7)

---

## 🚀 Next Steps

After testing with Postman:

1. **Implement Task 2.6:** Conversation Service
2. **Update collection:** Add conversation endpoints
3. **Build frontend:** Chat UI (Task 2.7)
4. **Integration testing:** End-to-end with frontend
5. **Deploy:** Test on staging/production

---

## 📞 Support

For issues or questions:

1. Check **POSTMAN-TESTING-GUIDE.md** for detailed help
2. Review **TASK-2.5-TEST-PLAN.md** for test scenarios
3. Check backend logs for errors
4. Verify database and Redis are running
5. Consult **QUERY-API-USAGE.md** for API details

---

## 📝 Feedback

If you find issues with the collection:

1. Check if backend is running latest code
2. Verify environment variables are set correctly
3. Review backend logs for errors
4. Check database state
5. Test with curl to isolate Postman issues

---

**Happy Testing! 🎉**

For detailed instructions, see **POSTMAN-TESTING-GUIDE.md**
