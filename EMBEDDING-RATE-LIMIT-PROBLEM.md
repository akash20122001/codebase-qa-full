# CodeBase Q&A - Embedding Rate Limit Challenge

## Project Overview

**CodeBase Q&A** is a platform that allows developers to ask questions about their GitHub repositories using natural language. The system:

1. **Indexes repositories**: Clones GitHub repos, parses code files, chunks them into semantic units (functions, classes, methods)
2. **Generates embeddings**: Converts each code chunk into a 3072-dimensional vector using an embedding API
3. **Stores in vector database**: Saves chunks with embeddings in PostgreSQL with pgvector extension
4. **Answers questions**: Uses semantic search to find relevant code chunks and generates answers using an LLM

## Current Architecture

### Tech Stack
- **Backend**: Java 21, Spring Boot 3.2.5
- **Database**: PostgreSQL 15 with pgvector extension
- **Queue**: AWS SQS for async indexing jobs
- **Embedding API**: Google Gemini `gemini-embedding-001` (Free Tier)
- **LLM**: Google Gemini `gemini-1.5-flash`

### Indexing Flow
```
User connects repo → SQS message → Worker clones repo → 
Parse & chunk files → Batch embed chunks → Save to DB
```

### Current Chunking Strategy
- **Minimum chunk size**: 15 tokens (~60 chars) - captures small functions/hooks
- **Maximum chunk size**: 1500 tokens (~6000 chars) - splits large functions into sub-chunks
- **Languages supported**: Java, TypeScript, JavaScript, Python, Go, Rust, etc.
- **Chunking quality**: 85-95% accuracy in extracting function/class names

### Current Embedding Configuration
- **Batch size**: 10 chunks per API call
- **Delay between batches**: 15 seconds
- **Retry strategy**: Exponential backoff (2s, 4s, 8s, 16s, 32s) with jitter
- **Max retries**: 5 attempts

## The Problem: Rate Limiting

### Gemini Free Tier Limits
- **100 requests per minute (RPM)**
- **1,000 requests per day (RPD)**
- **Critical**: Batch API counts EACH item as a separate request (batch of 10 = 10 requests, not 1)

### Current Performance
- **Small repo** (~26 files, 40-50 chunks): 5 batches = 50 requests = ~75 seconds
- **Medium repo** (~100 files, 150-200 chunks): 20 batches = 200 requests = ~5 minutes
- **Large repo** (~500 files, 1000+ chunks): 100 batches = 1000 requests = ~25 minutes

### Issues Encountered
1. **429 Too Many Requests errors** even with 15-second delays due to rolling 60-second window
2. **Slow indexing**: 15-second delays make indexing painfully slow
3. **Daily quota exhaustion**: Testing/re-indexing same repo multiple times hits 1000 RPD limit
4. **Poor developer experience**: Can't iterate quickly during development

### Why We Can't Just Reduce Chunks
Lowering the minimum chunk size from 50 to 15 tokens was necessary because:
- TypeScript/JavaScript codebases have many small, focused functions (React hooks, utilities)
- Without capturing these, Q&A quality suffers - can't find functions like `useAuth()`, `useQuery()`
- Users asking "How does authentication work?" would get poor results

## Request for Solutions

**I need suggestions for solving the embedding rate limit problem while maintaining Q&A quality.**

### Constraints
- Must support semantic code search (vector embeddings required)
- Must handle repos with 500-1000+ code chunks
- Must maintain current chunking quality (function/class name extraction)
- Prefer solutions that work with free/low-cost tiers during development
- Open to architectural changes if they significantly improve the situation

### Areas to Explore

1. **Alternative Embedding APIs**
   - Are there embedding APIs with better free tier limits?
   - Which APIs offer good code embedding quality at reasonable cost?
   - Any APIs with true batch processing (1 API call for N items)?

2. **Caching & Optimization**
   - Can we cache embeddings for unchanged files across re-indexes?
   - Should we implement incremental indexing (only embed changed files)?
   - Any way to reduce embedding dimensions without losing quality?

3. **Architectural Changes**
   - Should we move to a different embedding model (open-source, self-hosted)?
   - Would a hybrid approach work (embed only key functions, not all code)?
   - Can we use a two-tier system (fast embeddings for search, detailed for context)?

4. **Rate Limit Strategies**
   - Better batching strategies to maximize throughput?
   - Should we implement a queue with rate-limit-aware scheduling?
   - Any way to parallelize across multiple API keys?

5. **Cost-Benefit Analysis**
   - What's the cost of paid Gemini tier vs alternatives?
   - At what scale does self-hosted embedding become viable?
   - What's the typical production cost for similar platforms?

### Current Code Structure

**Embedding Service Interface**:
```java
public interface EmbeddingService {
    float[] embedText(String text);
    List<float[]> embedBatch(List<String> texts);
    String toVectorString(float[] embedding);
    int getDimension();
}
```

**Current Implementation**: `GeminiEmbeddingService` using `gemini-embedding-001` via REST API

**Indexing Service**: Handles chunking, batching, and orchestration

## Questions for You

1. **What embedding API would you recommend** for a code Q&A platform with these requirements?
2. **What architectural changes** would you suggest to handle rate limits better?
3. **Are there proven patterns** for handling large-scale code embedding in production?
4. **What's the typical approach** for platforms like GitHub Copilot, Sourcegraph, or similar tools?
5. **Should we consider** moving away from cloud APIs to self-hosted models? If so, which ones?

Please provide:
- Specific API/service recommendations with pricing
- Architectural patterns or design changes
- Code-level optimization strategies
- Trade-offs and considerations for each approach
- Estimated costs for production scale (1000s of repos)
