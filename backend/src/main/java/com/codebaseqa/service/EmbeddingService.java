package com.codebaseqa.service;

import java.util.List;

/**
 * Interface for text embedding operations.
 * Allows swapping embedding providers without changing the indexing/query pipeline.
 */
public interface EmbeddingService {

    /**
     * Generate embedding for a single text.
     * @return float array (dimension depends on model — 768 for Gemini text-embedding-004)
     */
    float[] embedText(String text);

    /**
     * Batch embed multiple texts (more efficient than individual calls).
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * Convert float array to pgvector string format: "[0.1,0.2,0.3,...]"
     */
    String toVectorString(float[] embedding);

    /**
     * Get the embedding dimension for this provider.
     */
    int getDimension();
}
