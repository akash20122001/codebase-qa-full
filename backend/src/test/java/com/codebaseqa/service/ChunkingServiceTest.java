package com.codebaseqa.service;

import com.codebaseqa.service.ChunkingService.CodeChunkResult;
import com.codebaseqa.service.chunking.*;
import com.codebaseqa.service.impl.DefaultChunkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        // Manually create the service with all chunkers
        List<LanguageChunker> chunkers = List.of(
            new JavaChunker(),
            new TypeScriptChunker(),
            new PythonChunker(),
            new FallbackChunker()
        );
        chunkingService = new DefaultChunkingService(chunkers);
    }

    @Test
    void testJavaChunking() {
        String javaCode = """
            package com.example;
            
            public class Calculator {
                
                public int add(int a, int b) {
                    return a + b;
                }
                
                public int subtract(int a, int b) {
                    return a - b;
                }
                
                public int multiply(int a, int b) {
                    return a * b;
                }
            }
            """;

        List<CodeChunkResult> chunks = chunkingService.chunkFile(javaCode, "Calculator.java", "java");

        System.out.println("\n=== Java Chunking Test ===");
        System.out.println("Total chunks: " + chunks.size());
        
        for (CodeChunkResult chunk : chunks) {
            System.out.println("\nChunk: " + chunk.chunkName());
            System.out.println("Type: " + chunk.chunkType());
            System.out.println("Lines: " + chunk.startLine() + "-" + chunk.endLine());
            System.out.println("Content preview: " + chunk.content().substring(0, Math.min(50, chunk.content().length())) + "...");
        }

        // Assertions
        assertTrue(chunks.size() >= 3, "Should have at least 3 chunks (class + 3 methods or just 3 methods)");
        assertTrue(chunks.stream().anyMatch(c -> "add".equals(c.chunkName())), "Should have 'add' method");
        assertTrue(chunks.stream().anyMatch(c -> "subtract".equals(c.chunkName())), "Should have 'subtract' method");
        assertTrue(chunks.stream().anyMatch(c -> "multiply".equals(c.chunkName())), "Should have 'multiply' method");
    }

    @Test
    void testTypeScriptChunking() {
        String tsCode = """
            export class UserService {
                constructor(private db: Database) {}
                
                async getUser(id: string) {
                    return await this.db.findOne({ id });
                }
            }
            
            export function validateEmail(email: string): boolean {
                return email.includes('@');
            }
            
            const processData = async (data: any) => {
                return data.map(item => item.value);
            };
            """;

        List<CodeChunkResult> chunks = chunkingService.chunkFile(tsCode, "UserService.ts", "typescript");

        System.out.println("\n=== TypeScript Chunking Test ===");
        System.out.println("Total chunks: " + chunks.size());
        
        for (CodeChunkResult chunk : chunks) {
            System.out.println("\nChunk: " + chunk.chunkName());
            System.out.println("Type: " + chunk.chunkType());
            System.out.println("Lines: " + chunk.startLine() + "-" + chunk.endLine());
        }

        // Assertions
        assertTrue(chunks.size() >= 2, "Should have at least 2 chunks");
        assertTrue(chunks.stream().anyMatch(c -> "UserService".equals(c.chunkName())), "Should have 'UserService' class");
        assertTrue(chunks.stream().anyMatch(c -> "validateEmail".equals(c.chunkName()) || "processData".equals(c.chunkName())), 
            "Should have function chunks");
    }

    @Test
    void testPythonChunking() {
        String pythonCode = """
            class DataProcessor:
                def __init__(self, config):
                    self.config = config
                
                def process(self, data):
                    return [item * 2 for item in data]
            
            def calculate_sum(numbers):
                total = 0
                for num in numbers:
                    total += num
                return total
            
            async def fetch_data(url):
                response = await http.get(url)
                return response.json()
            """;

        List<CodeChunkResult> chunks = chunkingService.chunkFile(pythonCode, "processor.py", "python");

        System.out.println("\n=== Python Chunking Test ===");
        System.out.println("Total chunks: " + chunks.size());
        
        for (CodeChunkResult chunk : chunks) {
            System.out.println("\nChunk: " + chunk.chunkName());
            System.out.println("Type: " + chunk.chunkType());
            System.out.println("Lines: " + chunk.startLine() + "-" + chunk.endLine());
        }

        // Assertions
        assertTrue(chunks.size() >= 3, "Should have at least 3 chunks");
        assertTrue(chunks.stream().anyMatch(c -> "DataProcessor".equals(c.chunkName())), "Should have 'DataProcessor' class");
        assertTrue(chunks.stream().anyMatch(c -> "calculate_sum".equals(c.chunkName())), "Should have 'calculate_sum' function");
        assertTrue(chunks.stream().anyMatch(c -> "fetch_data".equals(c.chunkName())), "Should have 'fetch_data' function");
    }

    @Test
    void testFallbackChunking() {
        String unknownCode = """
            Some random text file content
            that doesn't match any known language.
            
            This should be chunked by the fallback chunker
            into block-based chunks.
            
            Another paragraph here with some content
            to make it long enough to be a valid chunk.
            We need at least 50 tokens worth of content
            for the chunk to be included in the results.
            """;

        List<CodeChunkResult> chunks = chunkingService.chunkFile(unknownCode, "README.txt", "unknown");

        System.out.println("\n=== Fallback Chunking Test ===");
        System.out.println("Total chunks: " + chunks.size());
        
        for (CodeChunkResult chunk : chunks) {
            System.out.println("\nChunk type: " + chunk.chunkType());
            System.out.println("Lines: " + chunk.startLine() + "-" + chunk.endLine());
            System.out.println("Content length: " + chunk.content().length() + " chars");
        }

        // Assertions
        assertFalse(chunks.isEmpty(), "Should have at least one chunk");
        assertTrue(chunks.stream().allMatch(c -> "BLOCK".equals(c.chunkType())), "All chunks should be type BLOCK");
    }

    @Test
    void testLanguageDetection() {
        System.out.println("\n=== Language Detection Test ===");
        
        assertEquals("java", chunkingService.detectLanguage("MyClass.java"));
        assertEquals("typescript", chunkingService.detectLanguage("component.ts"));
        assertEquals("typescript", chunkingService.detectLanguage("Component.tsx"));
        assertEquals("javascript", chunkingService.detectLanguage("script.js"));
        assertEquals("javascript", chunkingService.detectLanguage("Component.jsx"));
        assertEquals("python", chunkingService.detectLanguage("main.py"));
        assertEquals("go", chunkingService.detectLanguage("server.go"));
        assertEquals("rust", chunkingService.detectLanguage("main.rs"));
        assertEquals("unknown", chunkingService.detectLanguage("README.md"));
        
        System.out.println("✅ All language detections passed!");
    }

    @Test
    void testMinimumChunkSize() {
        // Very small code that should be filtered out
        String tinyCode = """
            public class Tiny {
                int x;
            }
            """;

        List<CodeChunkResult> chunks = chunkingService.chunkFile(tinyCode, "Tiny.java", "java");

        System.out.println("\n=== Minimum Chunk Size Test ===");
        System.out.println("Tiny code chunks: " + chunks.size());
        
        // Should either have no chunks (filtered) or fall back to block chunking
        assertTrue(chunks.isEmpty() || chunks.stream().allMatch(c -> "BLOCK".equals(c.chunkType())), 
            "Tiny chunks should be filtered or use fallback");
    }

    @Test
    void testComplexJavaClass() {
        String complexJava = """
            package com.codebaseqa.service;
            
            import java.util.List;
            
            public class RepoService {
                
                private final RepoRepository repoRepository;
                private final GitHubClient gitHubClient;
                
                public RepoService(RepoRepository repoRepository, GitHubClient gitHubClient) {
                    this.repoRepository = repoRepository;
                    this.gitHubClient = gitHubClient;
                }
                
                public Repo connectRepo(String repoName, User user) {
                    // Verify GitHub access
                    if (!gitHubClient.hasAccess(repoName, user.getGithubToken())) {
                        throw new AccessDeniedException("No access to repository");
                    }
                    
                    // Create repo
                    Repo repo = new Repo();
                    repo.setName(repoName);
                    repo.setUser(user);
                    repo.setStatus(RepoStatus.PENDING);
                    
                    return repoRepository.save(repo);
                }
                
                public List<Repo> getUserRepos(User user) {
                    return repoRepository.findByUserId(user.getId());
                }
                
                public void disconnectRepo(Long repoId, User user) {
                    Repo repo = repoRepository.findById(repoId)
                        .orElseThrow(() -> new NotFoundException("Repo not found"));
                    
                    if (!repo.getUser().getId().equals(user.getId())) {
                        throw new AccessDeniedException("Not your repo");
                    }
                    
                    repoRepository.delete(repo);
                }
            }
            """;

        List<CodeChunkResult> chunks = chunkingService.chunkFile(complexJava, "RepoService.java", "java");

        System.out.println("\n=== Complex Java Class Test ===");
        System.out.println("Total chunks: " + chunks.size());
        
        for (CodeChunkResult chunk : chunks) {
            System.out.println("\nChunk: " + chunk.chunkName());
            System.out.println("Type: " + chunk.chunkType());
            System.out.println("Lines: " + chunk.startLine() + "-" + chunk.endLine());
            System.out.println("Size: " + chunk.content().length() + " chars");
        }

        // Should have class + constructor + 3 methods = 5 chunks (or just the methods if class is filtered)
        assertTrue(chunks.size() >= 3, "Should have at least 3 method chunks");
        assertTrue(chunks.stream().anyMatch(c -> "connectRepo".equals(c.chunkName())), "Should have 'connectRepo' method");
        assertTrue(chunks.stream().anyMatch(c -> "getUserRepos".equals(c.chunkName())), "Should have 'getUserRepos' method");
        assertTrue(chunks.stream().anyMatch(c -> "disconnectRepo".equals(c.chunkName())), "Should have 'disconnectRepo' method");
    }
}
