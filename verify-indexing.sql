-- ========================================
-- Indexing Verification Queries
-- ========================================
-- Run these in PostgreSQL to verify indexing worked correctly

-- 1. Check all repositories
SELECT 
    id,
    full_name,
    status,
    total_chunks,
    last_indexed_at,
    created_at
FROM repos
ORDER BY created_at DESC;

-- 2. Check indexing jobs
SELECT 
    id,
    status,
    job_type,
    total_files,
    processed_files,
    progress,
    attempts,
    started_at,
    completed_at,
    error_message
FROM indexing_jobs
ORDER BY created_at DESC
LIMIT 10;

-- 3. Count chunks per repository
SELECT 
    r.full_name,
    r.status as repo_status,
    COUNT(c.id) as chunk_count,
    COUNT(DISTINCT c.file_path) as file_count,
    COUNT(DISTINCT c.language) as language_count
FROM repos r
LEFT JOIN code_chunks c ON c.repo_id = r.id
GROUP BY r.id, r.full_name, r.status
ORDER BY chunk_count DESC;

-- 4. Sample chunks from a repository
-- Replace 'YOUR_REPO_NAME' with actual repo name
SELECT 
    file_path,
    chunk_type,
    chunk_name,
    start_line,
    end_line,
    language,
    token_count,
    LEFT(content, 100) as content_preview,
    LEFT(embedding, 50) as embedding_preview
FROM code_chunks
WHERE repo_id = (SELECT id FROM repos WHERE full_name = 'octocat/Hello-World' LIMIT 1)
ORDER BY file_path, start_line
LIMIT 20;

-- 5. Check chunk types distribution
SELECT 
    r.full_name,
    c.chunk_type,
    COUNT(*) as count
FROM code_chunks c
JOIN repos r ON r.id = c.repo_id
GROUP BY r.full_name, c.chunk_type
ORDER BY r.full_name, count DESC;

-- 6. Check languages distribution
SELECT 
    r.full_name,
    c.language,
    COUNT(*) as count,
    AVG(c.token_count) as avg_tokens
FROM code_chunks c
JOIN repos r ON r.id = c.repo_id
GROUP BY r.full_name, c.language
ORDER BY r.full_name, count DESC;

-- 7. Find largest chunks (potential issues)
SELECT 
    r.full_name,
    c.file_path,
    c.chunk_name,
    c.token_count,
    c.end_line - c.start_line + 1 as line_count
FROM code_chunks c
JOIN repos r ON r.id = c.repo_id
ORDER BY c.token_count DESC
LIMIT 10;

-- 8. Verify embeddings are present
SELECT 
    COUNT(*) as total_chunks,
    COUNT(embedding) as chunks_with_embeddings,
    COUNT(*) - COUNT(embedding) as chunks_without_embeddings
FROM code_chunks;

-- 9. Check embedding dimensions (should be 768 for Gemini)
SELECT 
    file_path,
    chunk_name,
    array_length(string_to_array(TRIM(BOTH '[]' FROM embedding), ','), 1) as embedding_dimension
FROM code_chunks
WHERE embedding IS NOT NULL
LIMIT 5;

-- 10. Find failed jobs
SELECT 
    j.id,
    r.full_name,
    j.status,
    j.error_message,
    j.attempts,
    j.created_at,
    j.completed_at
FROM indexing_jobs j
JOIN repos r ON r.id = j.repo_id
WHERE j.status = 'FAILED'
ORDER BY j.created_at DESC;

-- 11. Check for orphaned chunks (chunks without repo)
SELECT COUNT(*) as orphaned_chunks
FROM code_chunks c
WHERE NOT EXISTS (SELECT 1 FROM repos r WHERE r.id = c.repo_id);

-- 12. Performance stats
SELECT 
    r.full_name,
    j.total_files,
    j.processed_files,
    r.total_chunks,
    EXTRACT(EPOCH FROM (j.completed_at - j.started_at)) as duration_seconds,
    ROUND(r.total_chunks::numeric / NULLIF(EXTRACT(EPOCH FROM (j.completed_at - j.started_at)), 0), 2) as chunks_per_second
FROM indexing_jobs j
JOIN repos r ON r.id = j.repo_id
WHERE j.status = 'COMPLETED'
  AND j.started_at IS NOT NULL
  AND j.completed_at IS NOT NULL
ORDER BY j.completed_at DESC
LIMIT 10;

-- ========================================
-- Quick Health Check (Run this first)
-- ========================================
SELECT 
    'Repos' as table_name,
    COUNT(*) as count
FROM repos
UNION ALL
SELECT 
    'Indexing Jobs',
    COUNT(*)
FROM indexing_jobs
UNION ALL
SELECT 
    'Code Chunks',
    COUNT(*)
FROM code_chunks
UNION ALL
SELECT 
    'Chunks with Embeddings',
    COUNT(*)
FROM code_chunks
WHERE embedding IS NOT NULL;

-- ========================================
-- Test Vector Search (if chunks exist)
-- ========================================
-- This tests if pgvector is working correctly
-- Replace the embedding with an actual embedding from your database

-- First, get a sample embedding:
-- SELECT embedding FROM code_chunks LIMIT 1;

-- Then test similarity search:
-- SELECT 
--     file_path,
--     chunk_name,
--     1 - (embedding <=> '[0.1,0.2,...]'::vector) as similarity
-- FROM code_chunks
-- WHERE repo_id = 'YOUR_REPO_ID'
-- ORDER BY embedding <=> '[0.1,0.2,...]'::vector
-- LIMIT 5;
