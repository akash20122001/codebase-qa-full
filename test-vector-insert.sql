-- Test script to verify vector casting works without consuming API quota
-- Run this directly in PostgreSQL to test the database fix

-- 1. Check if pgvector extension is enabled
SELECT * FROM pg_extension WHERE extname = 'vector';

-- 2. Check the current table structure
\d code_chunks

-- 3. Create a test vector (3072 dimensions - just first 10 for brevity)
-- In production, this would be the full 3072-dimensional array
DO $$
DECLARE
    test_vector TEXT;
    test_repo_id UUID;
    test_chunk_id UUID;
BEGIN
    -- Generate a simple test vector string (first 10 dimensions, rest zeros)
    test_vector := '[0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0';
    
    -- Add 3062 more zeros to make it 3072 dimensions
    FOR i IN 1..3062 LOOP
        test_vector := test_vector || ',0.0';
    END LOOP;
    
    test_vector := test_vector || ']';
    
    -- Get a real repo_id from your database (or create a test one)
    SELECT id INTO test_repo_id FROM repos LIMIT 1;
    
    IF test_repo_id IS NULL THEN
        RAISE NOTICE 'No repos found in database. Please connect a repo first.';
        RETURN;
    END IF;
    
    test_chunk_id := gen_random_uuid();
    
    -- 4. Test the INSERT with CAST (this is what our Java code does)
    RAISE NOTICE 'Testing INSERT with CAST to vector(3072)...';
    
    INSERT INTO code_chunks 
    (id, repo_id, file_path, start_line, end_line, chunk_type, chunk_name, 
     content, language, embedding, token_count, created_at)
    VALUES 
    (test_chunk_id, test_repo_id, 'test/file.java', 1, 10, 'FUNCTION', 'testFunction',
     'public void test() { }', 'java', CAST(test_vector AS vector(3072)), 100, NOW());
    
    RAISE NOTICE 'SUCCESS! Vector insert worked with CAST.';
    
    -- 5. Verify the insert
    RAISE NOTICE 'Verifying the inserted chunk...';
    
    IF EXISTS (SELECT 1 FROM code_chunks WHERE id = test_chunk_id) THEN
        RAISE NOTICE 'Chunk found in database!';
        RAISE NOTICE 'File path: test/file.java';
        RAISE NOTICE 'Embedding dimension: %', array_length(embedding::float[], 1) 
            FROM code_chunks WHERE id = test_chunk_id;
    ELSE
        RAISE NOTICE 'ERROR: Chunk not found after insert!';
    END IF;
    
    -- 6. Clean up test data
    RAISE NOTICE 'Cleaning up test chunk...';
    DELETE FROM code_chunks WHERE id = test_chunk_id;
    RAISE NOTICE 'Test completed successfully!';
    
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'ERROR: %', SQLERRM;
        RAISE NOTICE 'This means the vector casting is NOT working properly.';
END $$;

-- 7. Summary
SELECT 
    'Test completed. If you see SUCCESS above, the database fix is working!' as result;
