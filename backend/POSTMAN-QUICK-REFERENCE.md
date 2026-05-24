# Postman Quick Reference Card

## 🚀 Quick Setup (5 minutes)

1. **Import files into Postman:**
   - `CodebaseQA-API.postman_collection.json`
   - `CodebaseQA-Local.postman_environment.json`

2. **Start backend:**
   ```bash
   cd backend
   ./mvnw.cmd spring-boot:run
   ```

3. **Get JWT token:**
   - Open in browser: `http://localhost:8080/api/auth/github`
   - Copy token from response
   - Paste into Postman environment variable `jwt_token`

4. **Get repo ID:**
   - Send "List Repositories" request
   - Copy a repo ID with status "READY"
   - Paste into environment variable `repo_id`

5. **Ask a question:**
   - Send "Ask Question (New Conversation)" request
   - View SSE stream in response

---

## 📋 Essential Requests

| Request | Purpose | Expected Result |
|---------|---------|-----------------|
| **Health Check** | Verify backend is running | 200 OK, status: UP |
| **Get Current User** | Verify JWT token | User profile |
| **List Repositories** | Get repo IDs | Array of repos |
| **Ask Question** | Test RAG pipeline | SSE stream with citations, tokens, done |
| **Ask Follow-up** | Test conversation context | SSE stream with context-aware response |

---

## 🔑 Environment Variables

| Variable | Description | How to Get |
|----------|-------------|------------|
| `base_url` | Backend URL | Default: `http://localhost:8080` |
| `jwt_token` | Authentication token | GitHub OAuth or frontend |
| `repo_id` | Repository to query | List Repositories request |
| `conversation_id` | For follow-ups | From `done` event of previous query |
| `pending_repo_id` | For error testing | Newly connected repo (not READY) |

---

## 📊 SSE Response Format

```
event: citations
data: [{"filePath":"...","startLine":15,"endLine":42,...}]

event: token
data: {"content":"The"}

event: token
data: {"content":" authentication"}

...

event: done
data: {"messageId":"...","conversationId":"...","tokenCount":342}
```

**Save the `conversationId` for follow-up questions!**

---

## ✅ Test Checklist

### Basic Tests
- [ ] Health check works
- [ ] Authentication works (Get Current User)
- [ ] List repositories returns data
- [ ] Ask question returns SSE stream
- [ ] Follow-up question uses context
- [ ] Second identical query is cached (faster)

### Error Tests
- [ ] Query non-indexed repo → REPO_NOT_INDEXED
- [ ] Query with >1000 chars → 400 validation error
- [ ] Query without JWT → 401 Unauthorized
- [ ] 21st query → RATE_LIMIT_EXCEEDED

---

## 🐛 Quick Troubleshooting

| Problem | Solution |
|---------|----------|
| 401 Unauthorized | Update `jwt_token` in environment |
| "repo_id not found" | Send List Repositories, copy a READY repo ID |
| REPO_NOT_INDEXED | Wait for indexing to complete (check status) |
| Rate limit hit | Clear Redis: `redis-cli DEL "ratelimit:query:<user-id>"` |
| No response | Check backend is running, verify Gemini API key |

---

## 🔍 Debug Commands

```bash
# Check backend health
curl http://localhost:8080/actuator/health

# Check Redis cache
redis-cli KEYS "query:*"
redis-cli KEYS "ratelimit:*"

# Check database
psql -d codebaseqa -c "SELECT * FROM conversations ORDER BY created_at DESC LIMIT 5;"
psql -d codebaseqa -c "SELECT * FROM messages ORDER BY created_at DESC LIMIT 10;"

# View backend logs
tail -f backend/logs/application.log
```

---

## 📈 Expected Performance

| Operation | Time |
|-----------|------|
| Health check | <50ms |
| List repos | <100ms |
| First query | 2-5s |
| Cached query | <100ms |

---

## 🎯 Common Test Scenarios

### 1. Happy Path
```
1. List Repositories → Get repo_id
2. Ask Question → Get conversationId
3. Ask Follow-up → Uses context
4. Ask same question → Cached (fast)
```

### 2. Rate Limiting
```
1. Ask 20 questions → All succeed
2. Ask 21st question → RATE_LIMIT_EXCEEDED
3. Wait 1 hour or clear Redis → Works again
```

### 3. Error Handling
```
1. Query PENDING repo → REPO_NOT_INDEXED
2. Query with long question → 400 validation error
3. Query without token → 401 Unauthorized
```

---

## 📞 Need Help?

- **Full Guide:** `POSTMAN-TESTING-GUIDE.md`
- **Test Plan:** `TASK-2.5-TEST-PLAN.md`
- **API Reference:** `QUERY-API-USAGE.md`
- **Implementation:** `TASK-2.5-IMPLEMENTATION.md`

---

## 🎓 Pro Tips

1. **Use Collection Runner** for load testing (run 10+ iterations)
2. **Enable Visualize** to parse SSE stream (see full guide)
3. **Save conversationId** from each query for follow-ups
4. **Check backend logs** for detailed debugging
5. **Use Redis CLI** to inspect cache and rate limits
6. **Test caching** by sending identical questions twice
7. **Monitor circuit breaker** via health endpoint

---

## 🔗 Quick Links

- Backend: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- GitHub OAuth: `http://localhost:8080/api/auth/github`
- API Docs: `04-api-specification.md`

---

**Happy Testing! 🚀**
