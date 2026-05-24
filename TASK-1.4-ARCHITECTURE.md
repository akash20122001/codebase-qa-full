# Task 1.4: Repository CRUD - Architecture

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT (Browser/Postman)                 │
│                                                                   │
│  HTTP Request: POST /api/repos                                   │
│  Headers: Authorization: Bearer <JWT>                            │
│  Body: { "repoFullName": "owner/repo", "branch": "main" }       │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SPRING SECURITY FILTER CHAIN                  │
│                                                                   │
│  1. JwtAuthenticationFilter                                      │
│     - Extract JWT from Authorization header                      │
│     - Validate JWT signature                                     │
│     - Extract user ID from token                                 │
│     - Load User from database                                    │
│     - Set SecurityContext                                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                         CONTROLLER LAYER                         │
│                                                                   │
│  RepoController.connectRepo()                                    │
│  - @PostMapping("/api/repos")                                    │
│  - Validates request body (@Valid)                               │
│  - Gets authenticated user (@AuthenticationPrincipal)            │
│  - Calls RepoService                                             │
│  - Returns ApiResponse<ConnectRepoResponse>                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                          SERVICE LAYER                           │
│                                                                   │
│  RepoService.connectRepo()                                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 1. Verify GitHub Access                                   │  │
│  │    ├─→ GitHubClient.getRepository()                       │  │
│  │    │   └─→ GET https://api.github.com/repos/owner/repo   │  │
│  │    └─→ Returns: { id, fullName, defaultBranch }          │  │
│  │                                                            │  │
│  │ 2. Check Duplicate                                         │  │
│  │    └─→ RepoRepository.findByUserIdAndGithubRepoId()      │  │
│  │                                                            │  │
│  │ 3. Save Repo                                               │  │
│  │    └─→ RepoRepository.save()                              │  │
│  │        └─→ INSERT INTO repos (status = 'PENDING')         │  │
│  │                                                            │  │
│  │ 4. Create Indexing Job                                     │  │
│  │    └─→ IndexingJobRepository.save()                       │  │
│  │        └─→ INSERT INTO indexing_jobs (status = 'QUEUED')  │  │
│  │                                                            │  │
│  │ 5. Queue Job                                               │  │
│  │    └─→ SqsService.sendIndexingMessage()                   │  │
│  │        └─→ SQS.sendMessage({ jobId, repoId })            │  │
│  │                                                            │  │
│  │ 6. Return Result                                           │  │
│  │    └─→ ConnectRepoResult(repo, jobId)                     │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                       EXTERNAL SERVICES                          │
│                                                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐ │
│  │   GitHub API     │  │   PostgreSQL     │  │   AWS SQS    │ │
│  │                  │  │                  │  │              │ │
│  │  Verify access   │  │  Store repos     │  │  Queue jobs  │ │
│  │  Get repo info   │  │  Store jobs      │  │              │ │
│  └──────────────────┘  └──────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## Component Interaction Diagram

```
┌──────────────┐
│   Client     │
└──────┬───────┘
       │ POST /api/repos
       │ + JWT Token
       ▼
┌──────────────────────┐
│  RepoController      │
│  ┌────────────────┐  │
│  │ @PostMapping   │  │
│  │ @Valid         │  │
│  │ @AuthPrincipal │  │
│  └────────┬───────┘  │
└───────────┼──────────┘
            │
            ▼
┌───────────────────────────────────────────────────────┐
│  RepoService                                          │
│  ┌─────────────────────────────────────────────────┐ │
│  │  @Transactional                                  │ │
│  │  connectRepo(request, user)                      │ │
│  └─────────────────────────────────────────────────┘ │
│                                                        │
│  Dependencies:                                         │
│  ├─→ GitHubClient      (verify access)               │
│  ├─→ RepoRepository    (save repo)                   │
│  ├─→ JobRepository     (create job)                  │
│  └─→ SqsService        (queue job)                   │
└───────────────────────────────────────────────────────┘
            │
            ├──────────────────────────────────┐
            │                                   │
            ▼                                   ▼
┌─────────────────────┐           ┌─────────────────────┐
│  GitHubClient       │           │  SqsService         │
│  ┌───────────────┐  │           │  ┌───────────────┐  │
│  │ WebClient     │  │           │  │ SqsClient     │  │
│  │ GET /repos/   │  │           │  │ sendMessage() │  │
│  └───────────────┘  │           │  └───────────────┘  │
└─────────────────────┘           └─────────────────────┘
            │                                   │
            ▼                                   ▼
┌─────────────────────┐           ┌─────────────────────┐
│  GitHub API         │           │  AWS SQS            │
│  api.github.com     │           │  Queue: indexing    │
└─────────────────────┘           └─────────────────────┘
```

---

## Data Flow

### 1. Connect Repository Request

```
Client Request
    ↓
{
  "repoFullName": "octocat/Hello-World",
  "branch": "main"
}
    ↓
Validation (Jakarta Validation)
    ↓
Pattern: ^[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+$
    ↓
RepoService.connectRepo()
    ↓
GitHubClient.getRepository()
    ↓
GitHub API Response
    ↓
{
  "id": 1296269,
  "full_name": "octocat/Hello-World",
  "default_branch": "master"
}
    ↓
Database Insert (repos)
    ↓
{
  id: UUID,
  user_id: UUID,
  github_repo_id: 1296269,
  full_name: "octocat/Hello-World",
  default_branch: "master",
  status: "PENDING"
}
    ↓
Database Insert (indexing_jobs)
    ↓
{
  id: UUID,
  repo_id: UUID,
  status: "QUEUED",
  job_type: "FULL"
}
    ↓
SQS Message
    ↓
{
  "jobId": "770e8400-...",
  "repoId": "660e8400-..."
}
    ↓
Response to Client (202 Accepted)
    ↓
{
  "data": {
    "id": "660e8400-...",
    "fullName": "octocat/Hello-World",
    "branch": "master",
    "status": "PENDING",
    "jobId": "770e8400-...",
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## Database Schema

```sql
┌─────────────────────────────────────────────────────────┐
│                         users                            │
├─────────────────────────────────────────────────────────┤
│ id (UUID, PK)                                            │
│ github_id (BIGINT, UNIQUE)                               │
│ username (VARCHAR)                                       │
│ email (VARCHAR)                                          │
│ avatar_url (VARCHAR)                                     │
│ github_token (TEXT)  ← Used for GitHub API calls        │
│ created_at (TIMESTAMP)                                   │
│ updated_at (TIMESTAMP)                                   │
└────────────────┬────────────────────────────────────────┘
                 │
                 │ 1:N
                 │
┌────────────────▼────────────────────────────────────────┐
│                         repos                            │
├─────────────────────────────────────────────────────────┤
│ id (UUID, PK)                                            │
│ user_id (UUID, FK → users.id)                           │
│ github_repo_id (BIGINT)                                  │
│ full_name (VARCHAR)  ← "owner/repo"                     │
│ default_branch (VARCHAR)                                 │
│ status (VARCHAR)  ← PENDING, INDEXING, READY, FAILED    │
│ webhook_id (BIGINT, NULL)                                │
│ total_chunks (INT, DEFAULT 0)                            │
│ last_indexed_at (TIMESTAMP, NULL)                        │
│ created_at (TIMESTAMP)                                   │
│ updated_at (TIMESTAMP)                                   │
└────────────────┬────────────────────────────────────────┘
                 │
                 │ 1:N
                 │
┌────────────────▼────────────────────────────────────────┐
│                    indexing_jobs                         │
├─────────────────────────────────────────────────────────┤
│ id (UUID, PK)                                            │
│ repo_id (UUID, FK → repos.id)                           │
│ status (VARCHAR)  ← QUEUED, PROCESSING, COMPLETED, ...  │
│ job_type (VARCHAR)  ← FULL, INCREMENTAL                 │
│ attempts (INT, DEFAULT 0)                                │
│ total_files (INT, NULL)                                  │
│ processed_files (INT, DEFAULT 0)                         │
│ progress (INT, DEFAULT 0)  ← Percentage                  │
│ error_message (TEXT, NULL)                               │
│ started_at (TIMESTAMP, NULL)                             │
│ completed_at (TIMESTAMP, NULL)                           │
│ created_at (TIMESTAMP)                                   │
└─────────────────────────────────────────────────────────┘
```

---

## Security Flow

```
┌──────────────────────────────────────────────────────────┐
│  1. Client sends request with JWT                        │
│     Authorization: Bearer eyJhbGciOiJIUzI1NiIs...        │
└────────────────────┬─────────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────────┐
│  2. JwtAuthenticationFilter intercepts                   │
│     - Extract token from header                          │
│     - Validate signature with secret key                 │
│     - Extract user ID from "sub" claim                   │
└────────────────────┬─────────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────────┐
│  3. Load User from database                              │
│     UserRepository.findById(userId)                      │
└────────────────────┬─────────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────────┐
│  4. Set SecurityContext                                  │
│     SecurityContextHolder.setAuthentication(user)        │
└────────────────────┬─────────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────────┐
│  5. Controller receives authenticated User               │
│     @AuthenticationPrincipal User user                   │
└────────────────────┬─────────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────────┐
│  6. Service verifies ownership                           │
│     RepoRepository.findByIdAndUserId(repoId, userId)     │
└──────────────────────────────────────────────────────────┘
```

---

## Error Handling Flow

```
┌─────────────────────────────────────────────────────────┐
│  Request: POST /api/repos                                │
│  Body: { "repoFullName": "private/repo" }               │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  GitHubClient.getRepository()                            │
│  GET https://api.github.com/repos/private/repo          │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  GitHub API Response: 403 Forbidden                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  GitHubClient catches WebClientResponseException         │
│  Throws: RuntimeException("You don't have access...")    │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Exception propagates to Controller                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Spring's @ControllerAdvice (if configured)              │
│  OR default error handler                                │
│  Returns: 500 Internal Server Error                      │
│  Body: { "error": { "message": "..." } }                │
└─────────────────────────────────────────────────────────┘
```

**Note**: Task 3.3 will add a proper `GlobalExceptionHandler` to return better error responses.

---

## Technology Stack

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                    │
│  - Spring MVC (@RestController)                         │
│  - Jackson (JSON serialization)                         │
│  - Jakarta Validation (request validation)              │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                     SERVICE LAYER                        │
│  - Spring @Service                                       │
│  - @Transactional (transaction management)              │
│  - Lombok (boilerplate reduction)                       │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                   PERSISTENCE LAYER                      │
│  - Spring Data JPA                                       │
│  - Hibernate (ORM)                                       │
│  - PostgreSQL (database)                                 │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                   INTEGRATION LAYER                      │
│  - WebClient (GitHub API)                                │
│  - AWS SDK v2 (SQS)                                      │
│  - Spring Security (authentication)                      │
└─────────────────────────────────────────────────────────┘
```

---

## Key Design Patterns Used

### 1. **Layered Architecture**
- Controller → Service → Repository
- Clear separation of concerns
- Each layer has specific responsibility

### 2. **DTO Pattern**
- Request DTOs for input validation
- Response DTOs for output formatting
- Separation from domain entities

### 3. **Repository Pattern**
- Spring Data JPA repositories
- Abstract database operations
- Custom query methods

### 4. **Dependency Injection**
- Constructor injection with Lombok's `@RequiredArgsConstructor`
- Loose coupling between components
- Easy testing with mocks

### 5. **Builder Pattern**
- Lombok's `@Builder` on entities
- Fluent API for object creation
- Immutable object construction

---

## What Happens Next?

```
Current State (After Task 1.4):
┌─────────────────────────────────────────────────────────┐
│  Repo connected → Job created → Message sent to SQS     │
│  Status: PENDING                                         │
└─────────────────────────────────────────────────────────┘

Future State (After Task 2.4 - SQS Worker):
┌─────────────────────────────────────────────────────────┐
│  Worker polls SQS → Processes job → Indexes code        │
│  Status: PENDING → INDEXING → READY                     │
└─────────────────────────────────────────────────────────┘

Complete Flow (After All Tasks):
┌─────────────────────────────────────────────────────────┐
│  1. Connect repo (Task 1.4)                              │
│  2. Worker indexes code (Task 2.4)                       │
│  3. User asks questions (Task 2.5)                       │
│  4. RAG pipeline retrieves relevant code (Task 2.5)      │
│  5. LLM generates answer (Task 2.5)                      │
│  6. Webhook updates on push (Task 3.2)                   │
└─────────────────────────────────────────────────────────┘
```

---

This architecture provides a solid foundation for the repository management system and sets up the infrastructure for the indexing pipeline that will be built in Sprint 2.
