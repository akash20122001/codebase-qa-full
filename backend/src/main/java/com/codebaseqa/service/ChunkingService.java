package com.codebaseqa.service;

import java.util.List;

/**
 * Interface for code chunking operations.
 * Splits source code files into logical chunks for embedding.
 */
public interface ChunkingService {

    /**
     * Parse a file into logical chunks based on its language.
     */
    List<CodeChunkResult> chunkFile(String content, String filePath, String language);

    /**
     * Detect programming language from file extension.
     */
    String detectLanguage(String filePath);

    /**
     * DTO for chunking results.
     */
    record CodeChunkResult(
        String content,
        String chunkName,
        String chunkType,  // FUNCTION, CLASS, METHOD, MODULE, BLOCK
        int startLine,
        int endLine
    ) {}
}
