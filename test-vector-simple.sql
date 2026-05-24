-- Simple test: Just try to insert a test vector
-- Replace YOUR_REPO_ID with an actual repo ID from your database

-- First, get a repo ID:
SELECT id, full_name FROM repos LIMIT 1;

-- Then run this (replace the UUID):
INSERT INTO code_chunks 
(id, repo_id, file_path, start_line, end_line, chunk_type, chunk_name, 
 content, language, embedding, token_count, created_at)
VALUES 
(gen_random_uuid(), 
 'YOUR_REPO_ID_HERE'::uuid,  -- Replace with actual repo ID
 'test/TestFile.java', 
 1, 
 10, 
 'FUNCTION', 
 'testFunction',
 'public void test() { System.out.println("test"); }', 
 'java', 
 CAST('[0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0' AS vector(3072)), 
 100, 
 NOW());

-- If this works without error, the fix is good!
-- Clean up:
-- DELETE FROM code_chunks WHERE file_path = 'test/TestFile.java';
