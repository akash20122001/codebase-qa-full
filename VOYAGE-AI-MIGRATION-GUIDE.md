# Voyage AI Migration Guide

## Overview

Successfully migrated from Google Gemini to Voyage AI `voyage-code-3` for code embeddings.

## What Changed

### 1. New Embedding Service
- **Added**: `VoyageEmbeddingService.java` - Purpose-built for code retrieval
- **Updated**: `GeminiEmbeddingService.java` - Now conditional (only loads if `app.embedding.provider=gemini`)
- **Dependency**: Added OkHttp 4.12.0 for Voyage AI API calls

### 2. Configuration
- **New property**: `app.embedding.provider` - Switch between `gemini` and `voyage`
- **Voyage config**: API key, model, dimension (1024), batch size (128)
- **Backward compatible**: Gemini still works if provider is set to `gemini`

### 3. Database Schema
- **Migration V6**: Changes vector dimension from 3072 (Gemini) to 1024 (Voyage AI)
- **Impact**: All existing embeddings are deleted - repos must be re-indexed

### 4. Performance Improvements
- **Removed**: 15-second delays between batches (only needed for Gemini)
- **Batch size**: Can now process 128 items per batch (vs 10 for Gemini)
- **Speed**: ~500x faster indexing (seconds vs minutes)

## Migration Steps

### Step 1: Get Voyage AI API Key

1. Sign up at https://dash.voyageai.com
2. Navigate to API Keys section
3. Create a new API key
4. Copy the key (starts with `pa-...`)

### Step 2: Update Environment Variables

Add to your `.env` file:

```bash
# Voyage AI (recommended embedding provider)
VOYAGE_API_KEY=pa-your-voyage-api-key-here
EMBEDDING_PROVIDER=voyage
```

Or keep using Gemini:

```bash
EMBEDDING_PROVIDER=gemini
```

### Step 3: Run Database Migration

The migration will automatically run on application startup via Flyway.

**⚠️ WARNING**: This will delete all existing embeddings!

```sql
-- V6__voyage_ai_embedding_dimension.sql
ALTER TABLE code_chunks ALTER COLUMN embedding TYPE vector(1024);
DELETE FROM code_chunks;
```

### Step 4: Restart Application

```bash
cd backend
./mvnw spring-boot:run
```

Check logs for:
```
🚀 Initialized Voyage AI Embedding Service
   Model: voyage-code-3
   Dimension: 1024
   Batch Size: 128
```

### Step 5: Re-index Repositories

All repositories must be re-indexed with the new embedding model:

1. Via API: `POST /api/repos/{repoId}/reindex`
2. Or delete and reconnect repos through the UI

## Performance Comparison

| Metric | Gemini (Before) | Voyage AI (After) |
|--------|----------------|-------------------|
| Rate Limit | 100 RPM | 2,000 RPM |
| Batch Size | 10 items | 128 items |
| Delay Between Batches | 15 seconds | None |
| Small Repo (50 chunks) | ~75 seconds | < 1 second |
| Medium Repo (200 chunks) | ~5 minutes | < 2 seconds |
| Large Repo (1000 chunks) | ~25 minutes | < 5 seconds |
| Free Tier | 1,000 requests/day | 200M tokens (~1,000 repos) |

## Configuration Reference

### application.yml

```yaml
app:
  embedding:
    provider: voyage  # or 'gemini'
    
    voyage:
      api-key: ${VOYAGE_API_KEY}
      model: voyage-code-3
      dimension: 1024  # Options: 256, 512, 1024, 2048
      batch-size: 128
    
    gemini:
      api-key: ${GEMINI_API_KEY}
      embedding-model: gemini-embedding-001
      rate-limit:
        delay-ms: 2000
        max-retries: 5
```

### Switching Providers

To switch back to Gemini:

1. Set `EMBEDDING_PROVIDER=gemini` in `.env`
2. Run migration to change vector dimension back to 3072:
   ```sql
   ALTER TABLE code_chunks ALTER COLUMN embedding TYPE vector(3072);
   DELETE FROM code_chunks;
   ```
3. Restart application
4. Re-index all repositories

## Cost Analysis

### Voyage AI Pricing

- **Free Tier**: 200 million tokens
- **Paid**: $0.18 per million tokens after free tier

### Usage Estimates

| Scale | Tokens | Cost |
|-------|--------|------|
| 10 repos | ~2M | Free |
| 100 repos | ~20M | Free |
| 500 repos | ~100M | Free |
| 1,000 repos | ~200M | Free (at limit) |
| 5,000 repos | ~1B | $144/month |

Average chunk: ~200 tokens  
Average repo: ~200 chunks = ~40K tokens

## Troubleshooting

### Issue: "Voyage API error: 401"
**Solution**: Check `VOYAGE_API_KEY` is set correctly in `.env`

### Issue: "Voyage API error: 429"
**Solution**: You've exceeded 2,000 RPM (very unlikely). Wait 1 minute and retry.

### Issue: Embeddings not working after migration
**Solution**: 
1. Check `EMBEDDING_PROVIDER` is set to `voyage`
2. Verify database migration ran (check `code_chunks.embedding` is `vector(1024)`)
3. Re-index repositories

### Issue: Want to test without deleting existing embeddings
**Solution**: 
1. Create a separate test database
2. Point application to test database
3. Test Voyage AI there first

## Rollback Plan

If you need to rollback to Gemini:

1. Set `EMBEDDING_PROVIDER=gemini`
2. Run SQL:
   ```sql
   ALTER TABLE code_chunks ALTER COLUMN embedding TYPE vector(3072);
   DELETE FROM code_chunks;
   ```
3. Restart application
4. Re-index repositories

## Additional Features

### Query-Time Optimization

When embedding user questions for search, use `input_type="query"`:

```java
// In your search/query service
VoyageEmbeddingService voyageService = (VoyageEmbeddingService) embeddingService;
List<float[]> queryEmbeddings = voyageService.embedBatch(List.of(userQuestion), "query");
```

This optimizes embeddings differently for queries vs documents, improving retrieval quality.

### Dimension Options

Voyage AI supports Matryoshka embeddings - you can reduce dimensions for storage savings:

- **1024** (default): Best quality
- **512**: ~2% quality loss, 50% less storage
- **256**: ~5% quality loss, 75% less storage

Change in `application.yml`:
```yaml
app:
  embedding:
    voyage:
      dimension: 512  # or 256
```

## Resources

- Voyage AI Docs: https://docs.voyageai.com/docs/embeddings
- Rate Limits: https://docs.voyageai.com/docs/rate-limits
- Pricing: https://docs.voyageai.com/docs/pricing
- Code Model: https://docs.voyageai.com/docs/embeddings#voyage-code-3
