# Testing the Vector Database Fix

This document explains how to test the vector database fix **without consuming your Gemini API quota**.

## Problem

The application was successfully embedding chunks but failing to save them to PostgreSQL with this error:
```
ERROR: column "embedding" is of type vector but expression is of type character varying
```

## Solution

We created a custom repository method `insertWithVectorCast()` that explicitly casts the string to `vector(3072)` type using native SQL.

## Testing Options

### Option 1: SQL Script (Fastest)

1. Connect to your PostgreSQL database:
   ```bash
   psql -U postgres -d codebaseqa
   ```

2. Run the simple test:
   ```bash
   \i test-vector-simple.sql
   ```

3. Or run the comprehensive test:
   ```bash
   \i test-vector-insert.sql
   ```

**Expected Result:** No errors, and you should see "SUCCESS!" messages.

### Option 2: Java Unit Test (Recommended)

Run the unit test that creates fake embeddings:

```bash
cd backend
./mvnw test -Dtest=ChunkRepositoryVectorTest
```

**Expected Result:**
```
✅ SUCCESS! Vector casting works correctly!
✅ SUCCESS! Multiple vector inserts work correctly!
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

### Option 3: Manual Database Test

1. Get a repo ID from your database:
   ```sql
   SELECT id, full_name FROM repos LIMIT 1;
   ```

2. Try inserting a test chunk:
   ```sql
   INSERT INTO code_chunks 
   (id, repo_id, file_path, start_line, end_line, chunk_type, chunk_name, 
    content, language, embedding, token_count, created_at)
   VALUES 
   (gen_random_uuid(), 
    'YOUR_REPO_ID'::uuid,
    'test/Test.java', 
    1, 10, 'FUNCTION', 'test',
    'public void test() {}', 
    'java', 
    CAST('[0.1,0.2,0.3]' AS vector(3072)),  -- Simplified for testing
    100, 
    NOW());
   ```

3. If no error, the fix works! Clean up:
   ```sql
   DELETE FROM code_chunks WHERE file_path = 'test/Test.java';
   ```

## What Changed

### Before (BROKEN)
```java
@Column(nullable = false, columnDefinition = "vector(3072)")
@JdbcTypeCode(SqlTypes.VARCHAR)  // Hibernate still tried to convert
private String embedding;

// Hibernate generated:
// INSERT ... VALUES (..., '[0.1,0.2,...]', ...)
// PostgreSQL rejected: "character varying" cannot be cast to "vector"
```

### After (FIXED)
```java
// Custom repository method with explicit CAST
@Query(value = """
    INSERT INTO code_chunks (..., embedding, ...)
    VALUES (..., CAST(:embedding AS vector(3072)), ...)
    """, nativeQuery = true)
void insertWithVectorCast(...);

// PostgreSQL receives:
// INSERT ... VALUES (..., CAST('[0.1,0.2,...]' AS vector(3072)), ...)
// PostgreSQL accepts: explicit cast to vector type
```

## Verification Checklist

- [ ] SQL test runs without errors
- [ ] Java unit test passes (2/2 tests)
- [ ] No "character varying" errors in logs
- [ ] Test chunks are inserted and can be queried
- [ ] Ready to test with real repo (will consume API quota)

## Next Steps

Once tests pass:

1. **Clean up database:**
   ```sql
   DELETE FROM repos WHERE full_name = 'akash20122001/project-bolt';
   ```

2. **Restart backend** to load the new code

3. **Connect repo** - should now work end-to-end:
   - ✅ Clone repo
   - ✅ Parse files
   - ✅ Embed chunks (consumes quota)
   - ✅ Save to database (now works!)
   - ✅ Complete successfully

## Troubleshooting

### Test fails with "relation does not exist"
- Run Flyway migrations: `./mvnw flyway:migrate`
- Ensure database is running: `docker ps` or check PostgreSQL service

### Test fails with "vector type does not exist"
- Install pgvector extension:
  ```sql
  CREATE EXTENSION IF NOT EXISTS vector;
  ```

### Test passes but real indexing still fails
- Ensure you restarted the backend after code changes
- Check that the new `insertWithVectorCast` method is being called
- Look for "Saving X chunks to database with vector casting" in logs

## Performance Note

The custom insert method loops through chunks individually instead of using `saveAll()`. For 93 chunks, this adds ~1 second to save time, which is acceptable given it prevents the entire indexing job from failing.

Future optimization: Could batch the inserts using JDBC batch operations if needed.
