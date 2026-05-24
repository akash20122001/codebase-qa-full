package com.codebaseqa.service.impl;

import com.codebaseqa.service.ChunkingService;
import com.codebaseqa.service.chunking.LanguageChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultChunkingService implements ChunkingService {

    // Spring auto-injects all LanguageChunker implementations
    private final List<LanguageChunker> chunkers;

    private static final int MIN_CHUNK_TOKENS = 15; // ~60 chars, captures even tiny hooks/utils
    private static final int MAX_CHUNK_TOKENS = 1500; // ~6000 chars, safe for embedding models
    private static final int CHARS_PER_TOKEN = 4;

    @Override
    public List<CodeChunkResult> chunkFile(String content, String filePath, String language) {
        // Strategy selection — find the right chunker for this language
        // Sort by @Order annotation to ensure specific chunkers are checked before FallbackChunker
        LanguageChunker chunker = chunkers.stream()
            .sorted((a, b) -> {
                Order orderA = a.getClass().getAnnotation(Order.class);
                Order orderB = b.getClass().getAnnotation(Order.class);
                int priorityA = orderA != null ? orderA.value() : Integer.MAX_VALUE;
                int priorityB = orderB != null ? orderB.value() : Integer.MAX_VALUE;
                return Integer.compare(priorityA, priorityB);
            })
            .filter(c -> c.supports(language))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No chunker found for language: " + language));

        log.info("🔍 Chunking file: {} with language: {} using {}", filePath, language, chunker.getClass().getSimpleName());

        List<CodeChunkResult> chunks = chunker.chunk(content);
        
        log.info("📊 Chunker returned {} chunks before filtering", chunks.size());
        if (!chunks.isEmpty()) {
            log.info("   First chunk: name={}, type={}", chunks.get(0).chunkName(), chunks.get(0).chunkType());
        }

        // Filter out chunks that are too small or too large
        List<CodeChunkResult> validChunks = new ArrayList<>();
        for (CodeChunkResult chunk : chunks) {
            int tokenCount = chunk.content().length() / CHARS_PER_TOKEN;
            
            if (tokenCount < MIN_CHUNK_TOKENS) {
                continue; // Too small, skip
            }
            
            if (tokenCount <= MAX_CHUNK_TOKENS) {
                validChunks.add(chunk); // Perfect size
            } else {
                // Too large, split into sub-chunks
                log.warn("⚠️ Chunk '{}' is too large ({} tokens), splitting...", chunk.chunkName(), tokenCount);
                validChunks.addAll(splitLargeChunk(chunk));
            }
        }
        
        chunks = validChunks;
        
        log.info("📊 After size filtering: {} chunks remain", chunks.size());

        // If no valid chunks, fall back to block chunking
        if (chunks.isEmpty()) {
            log.warn("⚠️ No valid chunks found, falling back to FallbackChunker");
            LanguageChunker fallback = chunkers.stream()
                .filter(c -> c.getClass().getSimpleName().equals("FallbackChunker"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("FallbackChunker not found"));
            chunks = fallback.chunk(content);
        }

        return chunks;
    }

    /**
     * Split a large chunk into smaller sub-chunks while preserving context.
     */
    private List<CodeChunkResult> splitLargeChunk(CodeChunkResult largeChunk) {
        List<CodeChunkResult> subChunks = new ArrayList<>();
        String[] lines = largeChunk.content().split("\n");
        int maxCharsPerSubChunk = MAX_CHUNK_TOKENS * CHARS_PER_TOKEN;
        
        StringBuilder currentSubChunk = new StringBuilder();
        int subChunkStartLine = largeChunk.startLine();
        int subChunkIndex = 1;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] + "\n";
            
            // If adding this line would exceed the limit, save current sub-chunk
            if (currentSubChunk.length() + line.length() > maxCharsPerSubChunk && currentSubChunk.length() > 0) {
                String subChunkName = largeChunk.chunkName() != null 
                    ? largeChunk.chunkName() + "#" + subChunkIndex
                    : null;
                    
                subChunks.add(new CodeChunkResult(
                    currentSubChunk.toString().stripTrailing(),
                    subChunkName,
                    largeChunk.chunkType(),
                    subChunkStartLine,
                    largeChunk.startLine() + i
                ));
                
                currentSubChunk = new StringBuilder();
                subChunkStartLine = largeChunk.startLine() + i;
                subChunkIndex++;
            }
            
            currentSubChunk.append(line);
        }
        
        // Add the last sub-chunk
        if (currentSubChunk.length() > 0) {
            String subChunkName = largeChunk.chunkName() != null 
                ? largeChunk.chunkName() + "#" + subChunkIndex
                : null;
                
            subChunks.add(new CodeChunkResult(
                currentSubChunk.toString().stripTrailing(),
                subChunkName,
                largeChunk.chunkType(),
                subChunkStartLine,
                largeChunk.endLine()
            ));
        }
        
        log.info("   Split into {} sub-chunks", subChunks.size());
        return subChunks;
    }

    @Override
    public String detectLanguage(String filePath) {
        String ext = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "java" -> "java";
            case "ts", "tsx" -> "typescript";
            case "js", "jsx" -> "javascript";
            case "py" -> "python";
            case "go" -> "go";
            case "rs" -> "rust";
            case "rb" -> "ruby";
            case "php" -> "php";
            case "cs" -> "csharp";
            case "kt" -> "kotlin";
            case "swift" -> "swift";
            case "scala" -> "scala";
            case "cpp", "cc", "cxx" -> "cpp";
            case "c", "h" -> "c";
            default -> "unknown";
        };
    }
}
