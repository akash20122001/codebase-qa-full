# Task 2.5 Testing Plan

## Prerequisites

Before testing, ensure:
1. PostgreSQL is running with pgvector extension
2. Redis is running
3. At least one repository is indexed (status = READY)
4. Valid JWT token is available
5. Gemini API key is configured in application.yml or .env

## Test Scenarios

### 1. Basic Query Flow (Happy Path)

**Setup:**
- User has a READY repository
- User has not exceeded rate limit

**Steps:**
1. Send POST request to `/api/query` with:
   ```json
   {
     "repoId": "<valid-repo-id>",
     "question": "How does the authentication work?"
   }
   ```
2. Observe SSE stream

**Expected Result:**
- First event: `citations` with array of code chunks
- Multiple `token` events with streaming text
- Final `done` event with messageId, conversationId, tokenCount
- Response is relevant to the question
- New conversation is created in database
- Two messages saved (user + assistant)

**Verification:**
```sql
-- Check conversation was created
SELECT * FROM conversations WHERE repo_id = '<repo-id>' ORDER BY created_at DESC LIMIT 1;

-- Check messages were saved
SELECT * FROM messages WHERE conversation_id = '<conversation-id>' ORDER BY created_at;
```

---

### 2. Follow-up Question (Conversation Context)

**Setup:**
- Complete Test 1 first to get a conversationId

**Steps:**
1. Send POST request with existing conversationId:
   ```json
   {
     "repoId": "<valid-repo-id>",
     "conversationId": "<from-test-1>",
     "question": "Can you explain that in more detail?"
   }
   ```

**Expected Result:**
- Response considers previous conversation context
- Messages are added to existing conversation
- Conversation's `updated_at` timestamp is updated

**Verification:**
```sql
-- Check messages count increased
SELECT COUNT(*) FROM messages WHERE conversation_id = '<conversation-id>';
-- Should be 4 (2 from test 1 + 2 from this test)
```

---

### 3. Rate Limiting

**Setup:**
- Fresh user or clear Redis cache for user

**Steps:**
1. Send 20 queries rapidly (within 1 hour)
2. Send 21st query

**Expected Result:**
- First 20 queries succeed
- 21st query returns SSE error event:
  ```json
  {
    "event": "error",
    "data": {
      "code": "RATE_LIMIT_EXCEEDED",
      "message": "You have exceeded the rate limit...",
      "retryAfter": <seconds>
    }
  }
  ```

**Verification:**
```bash
# Check Redis
redis-cli GET "ratelimit:query:<user-id>"
# Should return "21"

redis-cli TTL "ratelimit:query:<user-id>"
# Should return remaining seconds until reset
```

---

### 4. Cache Hit

**Setup:**
- Complete Test 1 first

**Steps:**
1. Send exact same question again:
   ```json
   {
     "repoId": "<same-repo-id>",
     "question": "How does the authentication work?"
   }
   ```

**Expected Result:**
- Response is much faster (no LLM call)
- Still returns valid SSE stream
- Citations may be empty for cached responses
- Answer is identical to first query

**Verification:**
- Check logs for "Cache hit for query on repo"
- Response time should be < 100ms

---

### 5. Repository Not Ready

**Setup:**
- Repository with status = PENDING or INDEXING

**Steps:**
1. Send query to non-ready repo:
   ```json
   {
     "repoId": "<pending-repo-id>",
     "question": "Any question"
   }
   ```

**Expected Result:**
- SSE error event:
  ```json
  {
    "event": "error",
    "data": {
      "code": "REPO_NOT_INDEXED",
      "message": "Repository is not ready yet. Current status: PENDING"
    }
  }
  ```

---

### 6. Unauthorized Access

**Setup:**
- Repository owned by different user

**Steps:**
1. Send query to another user's repo

**Expected Result:**
- SSE error event:
  ```json
  {
    "event": "error",
    "data": {
      "code": "FORBIDDEN",
      "message": "You don't have access to this repository"
    }
  }
  ```

---

### 7. Invalid Request

**Steps:**
1. Send request with missing repoId:
   ```json
   {
     "question": "Test"
   }
   ```

**Expected Result:**
- HTTP 400 Bad Request
- Validation error message

---

### 8. Circuit Breaker (Fault Tolerance)

**Setup:**
- Temporarily break Gemini API (invalid API key or network issue)

**Steps:**
1. Send 10 queries (to trigger circuit breaker)
2. Observe behavior

**Expected Result:**
- First few queries fail with LLM errors
- After 5 failures (50% of 10), circuit opens
- Subsequent queries fail immediately without calling LLM
- After 30 seconds, circuit goes to half-open
- One successful query closes the circuit

**Verification:**
- Check logs for circuit breaker state changes
- Check Actuator endpoint: `/actuator/health`

---

### 9. Long Question (Validation)

**Steps:**
1. Send question with > 1000 characters

**Expected Result:**
- HTTP 400 Bad Request
- Validation error: "Question must not exceed 1000 characters"

---

### 10. Vector Search Quality

**Setup:**
- Repository with diverse code files

**Steps:**
1. Ask specific question about a known function:
   ```json
   {
     "repoId": "<repo-id>",
     "question": "How does the JWT token validation work?"
   }
   ```

**Expected Result:**
- Citations include relevant files (e.g., JwtAuthenticationFilter.java)
- Citations have correct line numbers
- Code snippets are complete and readable
- Answer references the cited code

**Verification:**
- Manually check if cited files actually contain relevant code
- Verify line numbers are accurate

---

## Performance Benchmarks

### Expected Response Times:
- **First query (no cache):** 2-5 seconds (depends on LLM)
- **Cached query:** < 100ms
- **Rate limit check:** < 10ms
- **Vector search:** < 100ms

### Load Testing:
```bash
# Test concurrent queries (requires Apache Bench or similar)
ab -n 100 -c 10 -H "Authorization: Bearer <token>" \
   -p query.json -T application/json \
   http://localhost:8080/api/query
```

---

## Debugging Tips

### Enable Debug Logging:
```yaml
logging:
  level:
    com.codebaseqa.service.QueryService: DEBUG
    com.codebaseqa.service.impl.GeminiLlmService: DEBUG
```

### Check Redis Cache:
```bash
redis-cli KEYS "query:*"
redis-cli GET "query:<repo-id>:<hash>"
```

### Check Rate Limits:
```bash
redis-cli KEYS "ratelimit:*"
redis-cli GET "ratelimit:query:<user-id>"
```

### Monitor Circuit Breaker:
```bash
curl http://localhost:8080/actuator/health
```

### Check Database State:
```sql
-- Recent conversations
SELECT * FROM conversations ORDER BY created_at DESC LIMIT 10;

-- Recent messages
SELECT * FROM messages ORDER BY created_at DESC LIMIT 20;

-- Check citations are stored
SELECT id, role, citations FROM messages WHERE citations IS NOT NULL LIMIT 5;
```

---

## Common Issues & Solutions

### Issue: "Circuit breaker is open"
**Solution:** Wait 30 seconds or check Gemini API key

### Issue: "Rate limit exceeded" immediately
**Solution:** Clear Redis: `redis-cli DEL "ratelimit:query:<user-id>"`

### Issue: No relevant citations
**Solution:** 
- Check if repository is properly indexed
- Verify embeddings are stored: `SELECT COUNT(*) FROM code_chunks WHERE repo_id = '<repo-id>'`
- Check embedding dimension matches (should be 768 for Gemini or 1024 for Voyage)

### Issue: SSE connection drops
**Solution:**
- Check firewall/proxy settings
- Increase timeout in QueryController
- Check client-side SSE handling

### Issue: Slow responses
**Solution:**
- Check Gemini API latency
- Verify Redis is running (for cache)
- Check database query performance
- Consider reducing TOP_K_CHUNKS from 5 to 3

---

## Acceptance Criteria Checklist

From build plan Task 2.5:

- [ ] `POST /api/query` with a question returns streaming SSE response
- [ ] First event is `citations` with relevant code chunks
- [ ] Subsequent events are `token` with streamed text
- [ ] Final event is `done` with metadata
- [ ] Second identical query returns cached result (faster)
- [ ] 21st query in an hour returns 429 rate limit error
- [ ] QueryService depends on `LlmService` and `EmbeddingService` interfaces
- [ ] Prompt is built using `PromptBuilder.create().withCodeChunks(...).withQuestion(...).build()`

---

## Next Steps

After all tests pass:
1. Proceed to Task 2.6 (Conversation Service)
2. Implement conversation CRUD endpoints
3. Build frontend Chat UI (Task 2.7)
