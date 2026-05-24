-- Update embedding dimension from 768 to 3072 for gemini-embedding-001
-- The old text-embedding-004 model has been deprecated by Google
-- The new gemini-embedding-001 model outputs 3072-dimensional vectors

-- Update the vector dimension
ALTER TABLE code_chunks 
ALTER COLUMN embedding TYPE vector(3072);

-- Clear existing embeddings as they are incompatible (768 vs 3072 dimensions)
DELETE FROM code_chunks;

-- Note: All repositories will need to be re-indexed with the new model
