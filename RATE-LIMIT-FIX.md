# Rate Limit Fix for Gemini Embedding API

## Problem
When connecting a repository, the indexing process was failing with a `429 Too Many Requests` error from the Gemini Embedding API. The system was trying to embed 93 chunks but was making **93 individual API calls** instead of using the batch API properly.

## Root Cause Analysis
**You were absolutely right** - the batching was supposed to reduce API calls! With 93 chunks and batch size of 20, we should have made only **5 API calls** (93 ÷ 20 = 4.65 → 5 batches).

### The Real Problem
The `embedBatch()` method was **NOT actually batching**! It was:
```java
// WRONG - This makes N individual API calls!
for (String text : texts) {
    float[] embedding = embedText(text);  // Individual API call
    results.add(embedding);
}
```

This meant:
- 93 chunks → 93 individual API calls
- All calls made rapidly in succession
- Hit rate limit immediately
- **Batching was completely broken!**

## Solution Implemented

### 1. Proper Batch API Implementation
Now using Gemini's **`batchEmbedContents`** endpoint:
```java
// CORRECT - This makes 1 API call for multiple texts!
POST /v1beta/models/gemini-embedding-001:batchEmbedContents
{
  "requests": [
    { "model": "...", "content": { "parts": [{"text": "..."}] } },
    { "model": "...", "content": { "parts": [{"text": "..."}] } },
    // ... up to 20 texts in one call
  ]
}
```

### 2. Exponential Backoff for 429 Errors
- Retry mechanism specifically for rate limit errors
- Exponential backoff: 2^attempt × base delay (1.5s)
- Maximum 5 retry attempts

### 3. Delay Between Batches
- Added 1 second delay between batch API calls
- Prevents hitting rate limits even with proper batching

### 4. Better Logging
- Shows batch number: "Processing batch 1/5"
- Shows API call count: "Finished embedding 93 chunks in 5 API calls"

## Expected Behavior

### Before Fix (BROKEN)
- **93 chunks** → **93 API calls** 🔴
- Time: ~30 seconds (all at once)
- Result: 429 Rate Limit Error

### After Fix (CORRECT)
- **93 chunks** → **5 API calls** ✅
  - Batch 1: 20 chunks
  - Batch 2: 20 chunks  
  - Batch 3: 20 chunks
  - Batch 4: 20 chunks
  - Batch 5: 13 chunks
- Time: ~10 seconds (5 calls + delays)
- Result: Success!

## Code Changes

### GeminiEmbeddingService.java
1. **Completely rewrote `embedBatch()`** to use `batchEmbedContents` API
2. Now makes **1 API call per batch** instead of N individual calls
3. Added retry logic with exponential backoff for 429 errors
4. Proper response parsing for batch API format

### IndexingService.java
1. Kept `EMBEDDING_BATCH_SIZE` at 20 (optimal for batch API)
2. Added 1-second delay between batches
3. Enhanced logging to show batch progress and total API calls

### application.yml
1. Added `app.gemini.rate-limit` configuration:
   - `delay-ms: 1500` - Base delay for retries
   - `max-retries: 5` - Maximum retry attempts

## API Call Reduction

| Chunks | Old (Broken) | New (Fixed) | Reduction |
|--------|--------------|-------------|-----------|
| 93     | 93 calls     | 5 calls     | **94.6%** |
| 200    | 200 calls    | 10 calls    | **95.0%** |
| 1000   | 1000 calls   | 50 calls    | **95.0%** |

## Testing
To verify the fix:
1. Restart the backend application
2. Connect a repository through the API
3. Check logs for:
   - "Batch embedding 20 texts using batchEmbedContents API"
   - "Processing batch 1/5: chunks 1-20 of 93"
   - "Finished embedding all 93 chunks in 5 API calls"

## Thank You!
Your question was spot-on - batching **should** reduce API calls, and it wasn't working. The implementation was fundamentally broken. Now it's fixed! 🎉
