# Embedding Rate Limit Solution: Migrate to Voyage AI `voyage-code-3`

## Problem Summary

The current Gemini free tier (`gemini-embedding-001`) has severe limitations:
- **100 RPM** / **1,000 RPD** (requests per day)
- Each item in a batch counts as a separate request
- 15-second delays between batches → 25 minutes for a large repo
- Daily quota exhaustion during development/testing
- 429 errors even with exponential backoff

## Recommended Solution: Voyage AI `voyage-code-3`

### Why Voyage AI

| Aspect | Gemini Free Tier (current) | Voyage AI `voyage-code-3` |
|--------|---------------------------|---------------------------|
| Rate Limit | 100 RPM / 1,000 RPD | **2,000 RPM / 3M TPM** |
| Free Tier | 1,000 requests/day | **200 million tokens free** |
| Batch Handling | Each item = 1 request | True batch (128 items = 1 request) |
| Code Quality | General-purpose | **Purpose-built for code retrieval** |
| Dimensions | 3072 (fixed) | 256, 512, 1024 (default), 2048 |
| Context Window | 2048 tokens | **32,000 tokens** |
| Price (after free) | N/A | $0.18 / million tokens |
| Daily Limit | 1,000 requests | **None** |

### Performance Comparison

| Repo Size | Gemini (current) | Voyage AI (proposed) |
|-----------|-----------------|---------------------|
| Small (~50 chunks) | ~75 seconds | **< 1 second** |
| Medium (~200 chunks) | ~5 minutes | **< 2 seconds** |
| Large (~1000 chunks) | ~25 minutes | **< 5 seconds** |

### Free Tier Math

- Average chunk: ~200 tokens
- Large repo (1000 chunks): ~200K tokens per indexing
- **200M free tokens = ~1,000 full repo indexings for free**
- No daily request cap — iterate freely during development

---

## Implementation

### 1. Maven Dependency

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

### 2. Configuration

```yaml
embedding:
  provider: voyage  # switch from 'gemini' to 'voyage'
  voyage:
    api-key: ${VOYAGE_API_KEY}
    model: voyage-code-3
    dimension: 1024
    batch-size: 128
```

### 3. `VoyageEmbeddingService.java`

```java
@Service
@ConditionalOnProperty(name = "embedding.provider", havingValue = "voyage")
public class VoyageEmbeddingService implements EmbeddingService {

    private static final String API_URL = "https://api.voyageai.com/v1/embeddings";
    private static final String MODEL = "voyage-code-3";
    private static final int DIMENSION = 1024;
    private static final int BATCH_SIZE = 128;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public VoyageEmbeddingService(
            @Value("${embedding.voyage.api-key}") String apiKey,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public float[] embedText(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> allEmbeddings = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));
            allEmbeddings.addAll(callApi(batch));
        }
        return allEmbeddings;
    }

    private List<float[]> callApi(List<String> texts) {
        try {
            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "input", texts,
                    "input_type", "document",
                    "output_dimension", DIMENSION
            );
            RequestBody requestBody = RequestBody.create(
                    objectMapper.writeValueAsBytes(body),
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Voyage API error: " + response.code());
                }
                JsonNode root = objectMapper.readTree(response.body().bytes());
                List<float[]> embeddings = new ArrayList<>();
                for (JsonNode data : root.get("data")) {
                    JsonNode embedding = data.get("embedding");
                    float[] vec = new float[DIMENSION];
                    for (int j = 0; j < DIMENSION; j++) {
                        vec[j] = (float) embedding.get(j).asDouble();
                    }
                    embeddings.add(vec);
                }
                return embeddings;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to call Voyage API", e);
        }
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }

    @Override
    public String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        return sb.append("]").toString();
    }
}
```

### 4. Database Migration (3072 → 1024 dimensions)

```sql
-- Requires re-indexing all repositories after migration
ALTER TABLE code_chunks ALTER COLUMN embedding TYPE vector(1024);
```

### 5. Query-Time: Use `input_type = "query"`

When embedding user questions for search, pass `"input_type": "query"` instead of `"document"`. This improves retrieval quality as Voyage optimizes the embedding differently for queries vs documents.

---

## Additional Optimizations

### Incremental Indexing (avoid re-embedding unchanged files)

- Hash each file's content (SHA-256) before chunking
- Store the hash alongside chunks in the database
- On re-index, skip files whose hash hasn't changed
- Only embed new/modified chunks

### Remove Artificial Delays

With 2,000 RPM and true batch processing, you no longer need the 15-second delay between batches. A 1000-chunk repo needs only 8 requests — well within limits.

### Dimension Reduction for Storage Savings

Voyage `voyage-code-3` supports Matryoshka embeddings:
- **1024 dims** (default): best quality, recommended
- **512 dims**: ~2% quality loss, 50% less storage
- **256 dims**: ~5% quality loss, 75% less storage

For development/testing, 512 dimensions may be sufficient.

---

## Cost at Scale

| Scale | Tokens Used | Cost |
|-------|-------------|------|
| Dev/testing (10 repos) | ~2M | **Free** |
| 100 repos (~100K chunks) | ~20M | **Free** |
| 500 repos (~500K chunks) | ~100M | **Free** |
| 1000 repos (~1M chunks) | ~200M | **Free** (just at limit) |
| Beyond 200M tokens | — | $0.18 per million tokens |

---

## Migration Steps

1. Sign up at https://dash.voyageai.com and get API key
2. Add `VOYAGE_API_KEY` to environment/secrets
3. Implement `VoyageEmbeddingService` (code above)
4. Update `application.yml` to set `embedding.provider: voyage`
5. Run database migration to change vector dimension to 1024
6. Trigger re-indexing of all existing repositories
7. Remove the 15-second batch delay from indexing service
8. Update batch size from 10 to 128

---

## API Reference

- Docs: https://docs.voyageai.com/docs/embeddings
- Rate Limits: https://docs.voyageai.com/docs/rate-limits
- Pricing: https://docs.voyageai.com/docs/pricing
- Code model details: https://docs.voyageai.com/docs/embeddings#voyage-code-3
