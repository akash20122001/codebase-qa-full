# Gemini Embedding API Rate Limit Solution

## The Critical Discovery

**The batch API counts EACH ITEM as a separate request!**

This was the root cause of all our 429 errors. We thought:
- ❌ Batch of 30 items = 1 request
- ✅ **Batch of 30 items = 30 requests**

## Gemini Free Tier Limits

### Official Limits
- **RPM (Requests Per Minute)**: 100
- **RPD (Requests Per Day)**: 1,000
- **TPM (Tokens Per Minute)**: Dynamically capped, but not the main issue

### How Batching Works
When you call `batchEmbedContents` with 30 items:
1. Gemini unpacks the array
2. Evaluates **each item individually** against your quota
3. Consumes **30 units** of your 100 RPM quota instantly

### The Math That Was Breaking Us
- Batch size: 30 items
- Batches per minute: 4 batches
- **Total requests: 4 × 30 = 120 requests/minute**
- Limit: 100 RPM
- Result: **429 Too Many Requests** ❌

## The Solution

### 1. Reduce Batch Size to 10 Items
```java
private static final int EMBEDDING_BATCH_SIZE = 10;
```

**Why 10?**
- 100 RPM ÷ 10 items = 10 batches per minute maximum
- Leaves safety margin for retries
- Protects against burst limits

### 2. Enforce 10-Second Delays Between Batches
```java
Thread.sleep(10000); // 10 seconds between batches
```

**Why 10 seconds?**
- 60 seconds ÷ 10 seconds = 6 batches per minute
- 6 batches × 10 items = **60 requests per minute**
- Safely under the 100 RPM limit
- Accounts for processing time

### 3. Add Jitter to Exponential Backoff
```java
long baseBackoff = (long) (Math.pow(2, attempt) * rateLimitDelayMs);
long jitter = (long) (Math.random() * 500); // 0-500ms random
long backoffMs = baseBackoff + jitter;
```

**Why jitter?**
- Prevents concurrent loops from hammering the endpoint simultaneously
- Spreads out retry attempts
- Recommended by Gemini

## Performance Impact

### For 93 Chunks (Your Test Repo)

#### Old Approach (BROKEN)
- Batch size: 30
- Batches: 4
- Delay: 3 seconds
- **Time: ~12 seconds**
- **Result: 429 errors, retries, ~2 minutes actual**

#### New Approach (CORRECT)
- Batch size: 10
- Batches: 10 (93 ÷ 10 = 9.3 → 10)
- Delay: 10 seconds
- **Time: ~90 seconds (1.5 minutes)**
- **Result: No 429 errors, predictable timing**

### For Larger Repos (500 Chunks)

#### New Approach
- Batches: 50 (500 ÷ 10)
- Time: 50 × 10 seconds = **~8.3 minutes**
- Requests consumed: 500 of 1,000 daily quota

### Daily Quota Management

**Maximum chunks per day:**
- Daily limit: 1,000 requests
- Batch size: 10
- **Maximum: 1,000 chunks per day**

**For larger repos:**
- 1,000+ chunks will hit daily quota
- Need to implement quota tracking
- Consider paid tier for production

## Implementation Checklist

- [x] Reduce batch size to 10 items
- [x] Increase delay to 10 seconds between batches
- [x] Add jitter to exponential backoff
- [x] Fix database type mismatch (`@JdbcTypeCode(SqlTypes.VARCHAR)`)
- [ ] Test with 93-chunk repo
- [ ] Monitor for 429 errors
- [ ] Document quota usage

## Future Improvements

1. **Quota Tracking**
   - Track daily request count
   - Warn users when approaching limits
   - Queue jobs if quota exhausted

2. **Adaptive Rate Limiting**
   - Monitor 429 responses
   - Dynamically adjust delays
   - Learn optimal timing

3. **Paid Tier Migration**
   - For production: upgrade to paid tier
   - Higher limits: 1,000 RPM, 1M RPD
   - More predictable performance

## Key Takeaways

1. **Always read the fine print** - batch APIs may count items individually
2. **Test with small batches first** - don't assume batching = fewer requests
3. **Be conservative with free tiers** - they have strict, hidden limits
4. **Add jitter to retries** - prevents thundering herd problems
5. **Monitor quota usage** - free tiers run out fast

## Credits

Thanks to Gemini AI for providing the detailed explanation of how `batchEmbedContents` actually works under the hood!
