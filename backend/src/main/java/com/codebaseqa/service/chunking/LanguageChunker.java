package com.codebaseqa.service.chunking;

import com.codebaseqa.service.ChunkingService.CodeChunkResult;
import java.util.List;

/**
 * Strategy interface for language-specific code chunking.
 * Each implementation knows how to parse one language's syntax.
 */
public interface LanguageChunker {

    /**
     * Chunk the given source code content.
     */
    List<CodeChunkResult> chunk(String content);

    /**
     * Which languages this chunker supports.
     */
    boolean supports(String language);
}
