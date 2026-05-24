-- Update embedding dimension from 3072 (Gemini) to 1024 (Voyage AI voyage-code-3)
-- Voyage AI provides better code retrieval with smaller, more efficient embeddings
-- This migration is required when switching from Gemini to Voyage AI

-- Clear existing embeddings first (they are incompatible with new dimension)
DELETE FROM code_chunks;

-- Now update the vector dimension
ALTER TABLE code_chunks 
ALTER COLUMN embedding TYPE vector(1024);

-- Note: All repositories will need to be re-indexed with Voyage AI after this migration
