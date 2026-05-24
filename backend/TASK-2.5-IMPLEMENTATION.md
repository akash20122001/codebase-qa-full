# Task 2.5 Implementation Summary

## Overview
Implemented the complete RAG (Retrieval-Augmented Generation) pipeline with LLM integration, rate limiting, caching, and SSE streaming.

## Components Implemented

### 1. LlmService Interface
**File:** `src/main/java/com/codebaseqa/service/LlmService.java`

- Interface for LLM providers
- Allows swapping between different LLM services (Gemini, OpenAI, Claude, etc.)
- Single method: `streamChat()` with token-by-token streaming

### 2. GeminiLlmService Implementation
**File:** `src/main/java/com/codebaseqa/service/impl/GeminiLlmService.java`

- Concrete implementation using Google's Gemini API
- Integrated with Resilience4j circuit breaker for fault tolerance
- Streams responses using WebFlux
- Handles conversation history
- Configuration:
  - Model: `gemini-1.5-flash` (from application.yml)
  - Temperature: 0.3
  - Max tokens: 2048

### 3. PromptBuilder (Builder Pattern)
**File:** `src/main/java/com/codebaseqa/service/prompt/PromptBuilder.java`

- Builder pattern for constructing LLM prompts
- Assembles:
  - System instructions
  - Retrieved code context (with file paths, line numbers, chunk names)
  - Conversation history
  - User question
- Formats code snippets with markdown syntax highlighting
- Separates system prompt from user message for API compatibility

### 4. RateLimitService
**File:** `src/main/java/com/codebaseqa/service/RateLimitService.java`

- Token bucket rate limiting using Redis
- Limits: 20 queries per hour per user
- Methods:
  - `isRateLimited(userId)` - Check if user exceeded limit
  - `getRemainingQueries(userId)` - Get remaining quota
  - `getResetTime(userId)` - Get seconds until reset
- Automatic expiry using Redis TTL

### 5. QueryService (RAG Pipeline)
**File:** `src/main/java/com/codebaseqa/service/QueryService.java`

Complete RAG pipeline implementation:

**Pipeline Steps:**
1. **Rate Limiting** - Check if user exceeded query limit
2. **Validation** - Verify repo exists, user has access, repo is READY
3. **Cache Check** - Return cached result if available
4. **Conversation Management** - Get existing or create new conversation
5. **Embedding** - Convert question to vector using EmbeddingService
6. **Vector Search** - Find top 5 most relevant code chunks
7. **Citations** - Send code chunks to client first (SSE event)
8. **History** - Retrieve last 10 messages for context
9. **Prompt Building** - Use PromptBuilder to assemble prompt
10. **Save User Message** - Persist question to database
11. **LLM Streaming** - Stream response token-by-token
12. **Save Assistant Message** - Persist answer with citations
13. **Caching** - Cache result for future identical queries
14. **Done Event** - Send completion metadata

**SSE Events:**
- `citations` - Relevant code chunks (sent first)
- `token` - Individual tokens as they stream
- `done` - Final event with messageId, conversationId, tokenCount
- `error` - Error information with retry hints

**Error Handling:**
- `RATE_LIMIT_EXCEEDED` - User exceeded 20 queries/hour
- `REPO_NOT_INDEXED` - Repository not ready yet
- `FORBIDDEN` - User doesn't own the repository
- `INTERNAL_ERROR` - Unexpected errors

### 6. QueryController
**File:** `src/main/java/com/codebaseqa/controller/QueryController.java`

- REST endpoint: `POST /api/query`
- Produces: `text/event-stream` (SSE)
- Authentication: Required (JWT via @AuthenticationPrincipal)
- Timeout: 2 minutes
- Async processing using CompletableFuture
- Handles client disconnect, timeout, and errors

### 7. Request DTO
**File:** `src/main/java/com/codebaseqa/dto/request/AskQuestionRequest.java`

```java
{
  "repoId": "uuid",           // Required
  "conversationId": "uuid",   // Optional (for follow-ups)
  "question": "string"        // Required, max 1000 chars
}
```

## Design Patterns Used

### 1. Strategy Pattern
- `LlmService` interface allows swapping LLM providers
- `EmbeddingService` interface allows swapping embedding providers
- No changes needed in QueryService to switch providers

### 2. Builder Pattern
- `PromptBuilder` for constructing complex prompts
- Fluent API: `PromptBuilder.create().withCodeChunks(...).withQuestion(...).build()`
- Separates prompt construction logic from business logic

### 3. Circuit Breaker Pattern
- Resilience4j integration for both embedding and chat services
- Prevents cascading failures
- Configuration in application.yml:
  - Sliding window: 10 calls
  - Failure threshold: 50%
  - Wait duration: 30 seconds

## Dependencies on Other Services

### Existing Services Used:
- `EmbeddingService` - Convert text to vectors
- `CacheService` - Cache query results
- `ChunkRepository` - Vector similarity search
- `ConversationRepository` - Manage conversations
- `MessageRepository` - Store messages
- `RepoRepository` - Validate repositories

### New Services Created:
- `LlmService` - LLM abstraction
- `RateLimitService` - Query rate limiting
- `QueryService` - RAG pipeline orchestration

## Configuration

All configuration is in `application.yml`:

```yaml
app:
  gemini:
    api-key: ${GEMINI_API_KEY}
    chat-model: gemini-1.5-flash
    embedding-model: gemini-embedding-001
  
  rate-limit:
    queries-per-hour: 20

resilience4j:
  circuitbreaker:
    instances:
      gemini-chat:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
```

## Testing Checklist

### Manual Testing:
- [ ] `POST /api/query` with valid question returns SSE stream
- [ ] First event is `citations` with code chunks
- [ ] Subsequent events are `token` with streamed text
- [ ] Final event is `done` with metadata
- [ ] Second identical query returns cached result (faster)
- [ ] 21st query in an hour returns 429 rate limit error
- [ ] Query on non-indexed repo returns error
- [ ] Query on repo user doesn't own returns 403

### Integration Points:
- [ ] Works with existing authentication (JWT)
- [ ] Works with existing embedding service
- [ ] Works with existing cache service
- [ ] Creates conversations and messages correctly
- [ ] Vector search returns relevant chunks

## API Example

### Request:
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "repoId": "660e8400-e29b-41d4-a716-446655440001",
    "question": "How does the authentication middleware work?"
  }'
```

### Response (SSE Stream):
```
event: citations
data: [{"filePath":"src/auth/middleware.ts","startLine":15,"endLine":42,"chunkName":"authenticateUser","snippet":"..."}]

event: token
data: {"content":"The"}

event: token
data: {"content":" authentication"}

event: token
data: {"content":" middleware"}

...

event: done
data: {"messageId":"990e...","conversationId":"880e...","tokenCount":342}
```

## Next Steps (Task 2.6)

The following will be implemented in Task 2.6:
- `ConversationService` - CRUD operations for conversations
- `ConversationController` - REST endpoints for conversation management
- Wire conversation memory into QueryService (already partially done)

## Notes

1. **Conversation Creation**: QueryService automatically creates a new conversation if `conversationId` is not provided. The title is derived from the first question (truncated to 50 chars).

2. **Caching Strategy**: Cached responses are sent as a single token event (not streamed). This is intentional for performance.

3. **Rate Limiting**: Uses Redis for distributed rate limiting. Works across multiple backend instances.

4. **Circuit Breaker**: Protects against LLM API failures. After 5 consecutive failures (50% of 10 calls), circuit opens for 30 seconds.

5. **Token Counting**: Rough estimate (length / 4). For production, consider using a proper tokenizer.

6. **MAX_HISTORY_MESSAGES**: Currently unused but reserved for future optimization to limit context window size.

## Compilation Status

✅ All files compile successfully with no errors (only minor null-safety warnings)

## Files Created

1. `src/main/java/com/codebaseqa/service/LlmService.java`
2. `src/main/java/com/codebaseqa/service/impl/GeminiLlmService.java`
3. `src/main/java/com/codebaseqa/service/prompt/PromptBuilder.java`
4. `src/main/java/com/codebaseqa/service/RateLimitService.java`
5. `src/main/java/com/codebaseqa/service/QueryService.java`
6. `src/main/java/com/codebaseqa/controller/QueryController.java`
7. `src/main/java/com/codebaseqa/dto/request/AskQuestionRequest.java`

Total: 7 new files, ~800 lines of code
