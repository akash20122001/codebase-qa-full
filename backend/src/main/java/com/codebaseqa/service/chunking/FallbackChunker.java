package com.codebaseqa.service.chunking;

import com.codebaseqa.service.ChunkingService.CodeChunkResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fallback chunker for unsupported languages.
 * Splits by blank-line-separated blocks with a max token limit.
 */
@Component
@Order(Integer.MAX_VALUE) // Always last priority
public class FallbackChunker implements LanguageChunker {

    private static final int MAX_CHUNK_CHARS = 2000; // ~500 tokens

    @Override
    public boolean supports(String language) {
        return true; // Supports everything as fallback
    }

    @Override
    public List<CodeChunkResult> chunk(String content) {
        List<CodeChunkResult> chunks = new ArrayList<>();
        String[] lines = content.split("\n");

        int blockStart = 0;
        StringBuilder currentBlock = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            currentBlock.append(lines[i]).append("\n");

            boolean isBlockEnd = lines[i].trim().isEmpty() && currentBlock.length() >= MAX_CHUNK_CHARS;
            boolean isFileEnd = i == lines.length - 1;

            if (isBlockEnd || isFileEnd) {
                String blockContent = currentBlock.toString().stripTrailing();
                if (!blockContent.isBlank()) {
                    chunks.add(new CodeChunkResult(
                        blockContent, null, "BLOCK",
                        blockStart + 1, i + 1
                    ));
                }
                blockStart = i + 1;
                currentBlock = new StringBuilder();
            }
        }

        return chunks;
    }
}
