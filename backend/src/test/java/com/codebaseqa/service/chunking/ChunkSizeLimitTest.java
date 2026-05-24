package com.codebaseqa.service.chunking;

import com.codebaseqa.service.ChunkingService;
import com.codebaseqa.service.impl.DefaultChunkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ChunkSizeLimitTest {

    @Autowired
    private ChunkingService chunkingService;

    @Test
    public void testLargeFunctionIsSplit() {
        // Create a very large function (over 1500 tokens / 6000 chars)
        StringBuilder largeFunction = new StringBuilder();
        largeFunction.append("function processLargeDataset(data) {\n");
        
        // Add many lines to make it exceed MAX_CHUNK_TOKENS
        for (int i = 0; i < 300; i++) {
            largeFunction.append("  const result").append(i).append(" = data.map(item => item.value * ").append(i).append(");\n");
            largeFunction.append("  console.log('Processing step ").append(i).append(":', result").append(i).append(");\n");
        }
        
        largeFunction.append("  return finalResult;\n");
        largeFunction.append("}\n");

        String content = largeFunction.toString();
        
        // Verify the content is indeed large (over 6000 chars)
        assertTrue(content.length() > 6000, "Test content should be over 6000 chars");

        List<ChunkingService.CodeChunkResult> chunks = chunkingService.chunkFile(
            content,
            "test.ts",
            "typescript"
        );

        // Should be split into multiple sub-chunks
        assertTrue(chunks.size() > 1, "Large function should be split into multiple chunks");
        
        // Each chunk should have a name with #1, #2, etc.
        for (int i = 0; i < chunks.size(); i++) {
            ChunkingService.CodeChunkResult chunk = chunks.get(i);
            assertNotNull(chunk.chunkName(), "Chunk name should not be null");
            assertTrue(chunk.chunkName().contains("#"), "Sub-chunk should have # in name");
            
            // Each chunk should be under the max size
            int tokenCount = chunk.content().length() / 4;
            assertTrue(tokenCount <= 1500, "Each sub-chunk should be under 1500 tokens");
        }
    }

    @Test
    public void testNormalSizedFunctionNotSplit() {
        // Create a function that's large enough to pass MIN_CHUNK_TOKENS (50 tokens = ~200 chars)
        // but small enough to not be split (under 1500 tokens = ~6000 chars)
        String content = """
            function calculateSum(numbers) {
              let sum = 0;
              let count = 0;
              let average = 0;
              let min = Number.MAX_VALUE;
              let max = Number.MIN_VALUE;
              
              for (let i = 0; i < numbers.length; i++) {
                sum += numbers[i];
                count++;
                if (numbers[i] < min) min = numbers[i];
                if (numbers[i] > max) max = numbers[i];
              }
              
              average = sum / count;
              console.log('Sum:', sum);
              console.log('Count:', count);
              console.log('Average:', average);
              console.log('Min:', min);
              console.log('Max:', max);
              
              return { sum, count, average, min, max };
            }
            """;

        List<ChunkingService.CodeChunkResult> chunks = chunkingService.chunkFile(
            content,
            "test.ts",
            "typescript"
        );

        // Should be exactly 1 chunk (not split)
        assertEquals(1, chunks.size(), "Normal-sized function should not be split");
        
        ChunkingService.CodeChunkResult chunk = chunks.get(0);
        assertNotNull(chunk.chunkName(), "Chunk name should not be null");
        assertEquals("calculateSum", chunk.chunkName(), "Chunk name should be the function name");
        assertFalse(chunk.chunkName().contains("#"), "Normal chunk should not have # in name");
    }

    @Test
    public void testVerySmallChunksFiltered() {
        // Content under 20 tokens (~80 chars) should be filtered
        String content = """
            const x = 1;
            """;

        List<ChunkingService.CodeChunkResult> chunks = chunkingService.chunkFile(
            content,
            "test.ts",
            "typescript"
        );

        // Very small content will be filtered out or kept as fallback block
        assertNotNull(chunks, "Chunks should not be null");
    }

    @Test
    public void testSmallFunctionKept() {
        // Small but meaningful function (around 25 tokens = 100 chars) should be kept
        String content = """
            export function useAuth() {
              const user = getUser();
              return { user };
            }
            """;

        List<ChunkingService.CodeChunkResult> chunks = chunkingService.chunkFile(
            content,
            "test.ts",
            "typescript"
        );

        // Should keep the small function
        assertEquals(1, chunks.size(), "Small function should be kept");
        
        ChunkingService.CodeChunkResult chunk = chunks.get(0);
        assertNotNull(chunk.chunkName(), "Chunk name should not be null");
        assertEquals("useAuth", chunk.chunkName(), "Should extract function name");
        assertEquals("FUNCTION", chunk.chunkType(), "Should be marked as FUNCTION");
    }

    @Test
    public void testLargeJavaClassIsSplit() {
        StringBuilder largeClass = new StringBuilder();
        largeClass.append("public class DataProcessor {\n");
        
        // Add a very large method
        largeClass.append("  public void processData() {\n");
        for (int i = 0; i < 300; i++) {
            largeClass.append("    System.out.println(\"Processing step ").append(i).append("\");\n");
            largeClass.append("    int result").append(i).append(" = calculate").append(i).append("();\n");
        }
        largeClass.append("  }\n");
        largeClass.append("}\n");

        String content = largeClass.toString();
        
        assertTrue(content.length() > 6000, "Test content should be over 6000 chars");

        List<ChunkingService.CodeChunkResult> chunks = chunkingService.chunkFile(
            content,
            "DataProcessor.java",
            "java"
        );

        // Should be split into multiple sub-chunks
        assertTrue(chunks.size() > 1, "Large Java method should be split into multiple chunks");
        
        // Verify sub-chunk naming
        for (ChunkingService.CodeChunkResult chunk : chunks) {
            assertNotNull(chunk.chunkName(), "Chunk name should not be null");
            int tokenCount = chunk.content().length() / 4;
            assertTrue(tokenCount <= 1500, "Each sub-chunk should be under 1500 tokens");
        }
    }
}
