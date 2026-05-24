---
inclusion: auto
---

# Codebase Q&A - Project Architecture Reference

This steering file ensures all implementation work stays consistent with the project design.

## Quick Reference Links

**Core Documentation:**
- Project Overview: #[[file:01-README.md]]
- Architecture: #[[file:02-architecture.md]]
- Database Schema: #[[file:03-database-schema.md]]
- API Specification: #[[file:04-api-specification.md]]
- Infrastructure: #[[file:07-infrastructure.md]]
- Configuration: #[[file:08-configuration.md]]
- Build Plan: #[[file:09-build-plan.md]]

**Implementation Guides:**
- Backend Part 1 (Setup & Entities): #[[file:05-backend-guide-part1.md]]
- Backend Part 2 (Services): #[[file:05-backend-guide-part2.md]]
- Backend Part 3: #[[file:05-backend-guide-part3.md]]
- Backend Part 4: #[[file:05-backend-guide-part4.md]]
- Backend Part 5: #[[file:05-backend-guide-part5.md]]
- Backend Part 6: #[[file:05-backend-guide-part6.md]]
- Backend Part 7: #[[file:05-backend-guide-part7.md]]
- Frontend Part 1: #[[file:06-frontend-guide-part1.md]]
- Frontend Part 2: #[[file:06-frontend-guide-part2.md]]
- Frontend Part 3: #[[file:06-frontend-guide-part3.md]]
- Design System: #[[file:10-design-system.md]]

## Critical Design Principles

### Architecture Patterns
1. **Layered Architecture**: Controllers → Services → Repositories
2. **Strategy Pattern**: Language-specific chunking (JavaChunker, TypeScriptChunker, etc.)
3. **Builder Pattern**: PromptBuilder for LLM prompt construction
4. **Interface-Based Design**: EmbeddingService, LlmService, ChunkingService are interfaces

### Service Layer Rules
- **Concrete services** (no interface): AuthService, RepoService, QueryService, ConversationService, CacheService, RateLimitService, SqsService
- **Interface + Impl**: ChunkingService, EmbeddingService, LlmService
- All external API calls use circuit breakers (Resilience4j)

### Database
- PostgreSQL 15 with pgvector extension
- Vector embeddings: 768 dimensions (Gemini text-embedding-004)
- Flyway for migrations (V1, V2, V3)
- CASCADE deletes for repo disconnection

### Tech Stack
- **Backend**: Java 17, Spring Boot 3.2, Spring Data JPA, Spring Security + OAuth2
- **Frontend**: React 18, TypeScript, Vite, TanStack Query, Zustand, Tailwind CSS
- **Infrastructure**: AWS EC2, RDS PostgreSQL, SQS, SSM Parameter Store, Upstash Redis, Vercel
- **External APIs**: GitHub OAuth, Google Gemini (embeddings + chat)

### Key Features
1. GitHub OAuth authentication with JWT
2. AST-aware code chunking (tree-sitter)
3. Vector similarity search (pgvector)
4. RAG pipeline with streaming SSE responses
5. Conversation memory (last 10 messages)
6. Incremental re-indexing via webhooks
7. Rate limiting (20 queries/hour per user)
8. Circuit breakers for external APIs

## Implementation Checklist

When implementing any component, verify:
- [ ] Follows the layered architecture pattern
- [ ] Uses correct design patterns (Strategy, Builder, Interface-based)
- [ ] Includes proper error handling (GlobalExceptionHandler)
- [ ] Has circuit breaker for external API calls
- [ ] Implements rate limiting where applicable
- [ ] Uses proper JPA relationships and cascade rules
- [ ] Follows the API specification contracts
- [ ] Includes proper logging (Slf4j)
- [ ] Uses Lombok annotations appropriately
- [ ] Matches the database schema exactly

## Common Pitfalls to Avoid

1. **Don't create interfaces for simple services** - Only ChunkingService, EmbeddingService, and LlmService need interfaces
2. **Don't forget circuit breakers** - All Gemini API calls must use Resilience4j
3. **Don't skip rate limiting** - QueryController must check rate limits before processing
4. **Don't use blocking operations in SSE** - Use CompletableFuture for async processing
5. **Don't forget pgvector casting** - Native queries need `CAST(:embedding AS vector)`
6. **Don't skip webhook signature verification** - GitHub webhooks must verify HMAC SHA-256
7. **Don't forget cache invalidation** - Re-indexing must invalidate Redis cache for that repo

## Quick Command Reference

### Build & Run
```bash
# Backend
mvn clean package
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend
npm run dev
npm run build
```

### Database
```bash
# Run migrations
mvn flyway:migrate

# Connect to DB
psql -h localhost -U postgres -d codebaseqa
```

### Docker (Local Dev)
```bash
docker-compose up -d  # Start PostgreSQL + Redis
```

## Environment Variables Required

**Backend:**
- DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
- REDIS_URL
- JWT_SECRET (min 32 chars)
- GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, GITHUB_REDIRECT_URI
- GEMINI_API_KEY
- AWS_REGION, SQS_QUEUE_URL, SQS_DLQ_URL
- FRONTEND_URL
- INDEXING_TEMP_DIR

**Frontend:**
- VITE_API_URL

---

**Note**: This steering file uses file references (#[[file:...]]) to automatically include the full documentation when needed. The design documents will always be available during implementation.
