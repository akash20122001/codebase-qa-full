# Task 3.3: Global Exception Handler - Implementation Complete ✅

**Completed:** May 24, 2026  
**Status:** ✅ All acceptance criteria met

---

## Overview

Implemented a comprehensive global exception handling system that provides:
- Consistent error response format across all endpoints
- Proper HTTP status codes for different error scenarios
- Custom exception classes for specific business logic errors
- Centralized error handling with detailed logging

---

## What Was Built

### 1. Custom Exception Classes

Created 8 custom exception classes for different error scenarios:

| Exception | HTTP Status | Use Case |
|-----------|-------------|----------|
| `ResourceNotFoundException` | 404 | Resource not found (repo, conversation, etc.) |
| `UnauthorizedException` | 403 | User doesn't own the resource |
| `RateLimitExceededException` | 429 | Rate limit exceeded |
| `RepoNotReadyException` | 400 | Repository not ready for queries |
| `InvalidRequestException` | 400 | Invalid request data or business logic violation |
| `DuplicateResourceException` | 409 | Resource already exists |
| `ServiceUnavailableException` | 503 | External service unavailable (LLM, Embedding, GitHub) |

### 2. Global Exception Handler

**GlobalExceptionHandler.java** - Centralized exception handling with:

#### Handles Custom Exceptions:
- ✅ ResourceNotFoundException → 404
- ✅ UnauthorizedException → 403
- ✅ RateLimitExceededException → 429 (with Retry-After header)
- ✅ RepoNotReadyException → 400 (with current status)
- ✅ InvalidRequestException → 400
- ✅ DuplicateResourceException → 409
- ✅ ServiceUnavailableException → 503

#### Handles Spring/Framework Exceptions:
- ✅ AccessDeniedException → 403
- ✅ AuthenticationException → 401
- ✅ MethodArgumentNotValidException → 400 (validation errors)
- ✅ ConstraintViolationException → 400
- ✅ HttpMessageNotReadableException → 400 (malformed JSON)
- ✅ MethodArgumentTypeMismatchException → 400 (type mismatch)

#### Handles Circuit Breaker:
- ✅ CallNotPermittedException → 503 (circuit breaker open)

#### Fallback:
- ✅ Exception → 500 (unexpected errors)

### 3. Standard Error Response Format

**ErrorResponse.java** - Consistent error structure:

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Conversation not found with id: 123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/conversations/123e4567-e89b-12d3-a456-426614174000",
  "details": {
    "additionalInfo": "value"
  }
}
```

**Fields:**
- `code` - Error code for programmatic handling
- `message` - Human-readable error message
- `timestamp` - When the error occurred
- `path` - Request path that caused the error
- `details` - Optional additional information (map)

---

## Error Codes Reference

### Client Errors (4xx)

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `RESOURCE_NOT_FOUND` | 404 | Requested resource doesn't exist |
| `FORBIDDEN` | 403 | User doesn't have permission |
| `ACCESS_DENIED` | 403 | Spring Security access denied |
| `UNAUTHORIZED` | 401 | Authentication failed |
| `REPO_NOT_READY` | 400 | Repository not indexed yet |
| `INVALID_REQUEST` | 400 | Invalid request data |
| `VALIDATION_ERROR` | 400 | Field validation failed |
| `MALFORMED_REQUEST` | 400 | Invalid JSON syntax |
| `INVALID_PARAMETER` | 400 | Invalid parameter type |
| `DUPLICATE_RESOURCE` | 409 | Resource already exists |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |

### Server Errors (5xx)

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `SERVICE_UNAVAILABLE` | 503 | External service down |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## Example Error Responses

### 1. Resource Not Found (404)

**Request:**
```http
GET /api/conversations/invalid-uuid
Authorization: Bearer <token>
```

**Response:**
```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Conversation not found with id: invalid-uuid",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/conversations/invalid-uuid"
}
```

---

### 2. Unauthorized Access (403)

**Request:**
```http
GET /api/conversations/123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer <other-user-token>
```

**Response:**
```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Conversation not found with id: 123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/conversations/123e4567-e89b-12d3-a456-426614174000"
}
```

**Note:** Returns 404 instead of 403 to prevent information leakage (user can't tell if resource exists)

---

### 3. Rate Limit Exceeded (429)

**Request:**
```http
POST /api/query
Authorization: Bearer <token>
Content-Type: application/json

{
  "repoId": "660e8400-e29b-41d4-a716-446655440001",
  "question": "21st question in an hour"
}
```

**Response:**
```http
HTTP/1.1 429 Too Many Requests
Retry-After: 1823

{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "You have exceeded the rate limit. You can make 20 queries per hour.",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/query",
  "details": {
    "retryAfter": 1823
  }
}
```

---

### 4. Repository Not Ready (400)

**Request:**
```http
POST /api/query
Authorization: Bearer <token>
Content-Type: application/json

{
  "repoId": "pending-repo-id",
  "question": "How does auth work?"
}
```

**Response:**
```json
{
  "code": "REPO_NOT_READY",
  "message": "Repository is not ready for queries. Current status: PENDING",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/query",
  "details": {
    "currentStatus": "PENDING"
  }
}
```

---

### 5. Validation Error (400)

**Request:**
```http
POST /api/query
Authorization: Bearer <token>
Content-Type: application/json

{
  "repoId": "660e8400-e29b-41d4-a716-446655440001",
  "question": ""
}
```

**Response:**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed for one or more fields",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/query",
  "details": {
    "fieldErrors": {
      "question": "Question is required"
    }
  }
}
```

---

### 6. Malformed JSON (400)

**Request:**
```http
POST /api/query
Authorization: Bearer <token>
Content-Type: application/json

{
  "repoId": "not-a-valid-uuid",
  "question": "test"
}
```

**Response:**
```json
{
  "code": "MALFORMED_REQUEST",
  "message": "Invalid UUID format in request body.",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/query"
}
```

---

### 7. Service Unavailable (503)

**Request:**
```http
POST /api/query
Authorization: Bearer <token>
Content-Type: application/json

{
  "repoId": "660e8400-e29b-41d4-a716-446655440001",
  "question": "test"
}
```

**Response (when LLM service is down):**
```json
{
  "code": "SERVICE_UNAVAILABLE",
  "message": "LLM service is unavailable",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/query",
  "details": {
    "service": "LLM",
    "reason": "Circuit breaker is open"
  }
}
```

---

### 8. Internal Server Error (500)

**Response (for unexpected errors):**
```json
{
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred. Please try again later.",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/query"
}
```

---

## Updated Services

### Services Now Using Custom Exceptions:

1. **QueryService.java**
   - ✅ `ResourceNotFoundException` - Repo not found
   - ✅ `UnauthorizedException` - User doesn't own repo
   - ✅ `RepoNotReadyException` - Repo not ready
   - ✅ `RateLimitExceededException` - Rate limit exceeded
   - ✅ `ResourceNotFoundException` - Conversation not found

2. **ConversationServiceImpl.java**
   - ✅ `ResourceNotFoundException` - Conversation not found (get & delete)

---

## Benefits

### 1. Consistent Error Format
- All errors follow the same structure
- Easy for frontend to parse and display
- Programmatic error handling with error codes

### 2. Proper HTTP Status Codes
- RESTful API best practices
- Clients can handle errors based on status code
- Proper use of 4xx (client errors) vs 5xx (server errors)

### 3. Detailed Error Information
- Clear error messages for users
- Additional details for debugging
- Request path included for logging

### 4. Security
- Doesn't leak sensitive information
- Returns 404 instead of 403 for unauthorized access (prevents resource enumeration)
- Sanitized error messages

### 5. Maintainability
- Centralized error handling
- Easy to add new exception types
- Consistent logging

---

## Testing

### Test Scenarios

#### 1. Test Resource Not Found
```bash
# Invalid conversation ID
curl -X GET http://localhost:8080/api/conversations/invalid-uuid \
  -H "Authorization: Bearer <token>"

# Expected: 404 with RESOURCE_NOT_FOUND
```

#### 2. Test Unauthorized Access
```bash
# Try to access another user's conversation
curl -X GET http://localhost:8080/api/conversations/<other-user-conv-id> \
  -H "Authorization: Bearer <token>"

# Expected: 404 with RESOURCE_NOT_FOUND (not 403)
```

#### 3. Test Rate Limiting
```bash
# Make 21 queries rapidly
for i in {1..21}; do
  curl -X POST http://localhost:8080/api/query \
    -H "Authorization: Bearer <token>" \
    -H "Content-Type: application/json" \
    -d '{"repoId":"<repo-id>","question":"test"}' \
    -H "Accept: text/event-stream"
done

# Expected: First 20 succeed, 21st returns 429
```

#### 4. Test Repo Not Ready
```bash
# Query a pending repo
curl -X POST http://localhost:8080/api/query \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"repoId":"<pending-repo-id>","question":"test"}'

# Expected: 400 with REPO_NOT_READY
```

#### 5. Test Validation Error
```bash
# Empty question
curl -X POST http://localhost:8080/api/query \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"repoId":"<repo-id>","question":""}'

# Expected: 400 with VALIDATION_ERROR
```

#### 6. Test Malformed JSON
```bash
# Invalid UUID format
curl -X POST http://localhost:8080/api/query \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"repoId":"not-a-uuid","question":"test"}'

# Expected: 400 with MALFORMED_REQUEST
```

---

## Files Created

### Exception Classes
- `backend/src/main/java/com/codebaseqa/exception/ResourceNotFoundException.java`
- `backend/src/main/java/com/codebaseqa/exception/UnauthorizedException.java`
- `backend/src/main/java/com/codebaseqa/exception/RateLimitExceededException.java`
- `backend/src/main/java/com/codebaseqa/exception/RepoNotReadyException.java`
- `backend/src/main/java/com/codebaseqa/exception/InvalidRequestException.java`
- `backend/src/main/java/com/codebaseqa/exception/DuplicateResourceException.java`
- `backend/src/main/java/com/codebaseqa/exception/ServiceUnavailableException.java`

### Handler & Response
- `backend/src/main/java/com/codebaseqa/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/com/codebaseqa/dto/response/ErrorResponse.java`

### Documentation
- `backend/TASK-3.3-GLOBAL-EXCEPTION-HANDLER.md`

---

## Build Status

✅ **BUILD SUCCESS**
- 67 source files compiled (9 new files)
- All exception classes compile without errors
- Global exception handler integrated
- Services updated to use custom exceptions

---

## Next Steps

**Task 3.2: Webhook Integration (Incremental Re-indexing)**

Now that error handling is complete, the next task is to implement webhook integration for automatic re-indexing when code is pushed to GitHub.

**What to build:**
1. `WebhookController.java` - Receive GitHub push events
2. Webhook signature verification (HMAC SHA-256)
3. `processIncrementalIndexing()` in IndexingService
4. Webhook creation when connecting a repo

**Reference:** `docs/09-build-plan.md` Task 3.2

---

## Summary

Task 3.3 is complete! The application now has:
- ✅ Comprehensive error handling
- ✅ Consistent error response format
- ✅ Proper HTTP status codes
- ✅ Custom exceptions for business logic
- ✅ Centralized exception handling
- ✅ Detailed error logging
- ✅ Security-conscious error messages

The backend is now more robust and production-ready with proper error handling across all endpoints.
