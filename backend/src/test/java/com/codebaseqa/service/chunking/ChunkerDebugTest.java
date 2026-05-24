package com.codebaseqa.service.chunking;

import com.codebaseqa.service.ChunkingService.CodeChunkResult;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Debug test to see what chunks are actually being extracted.
 */
class ChunkerDebugTest {

    @Test
    void debugTypeScriptClass() {
        TypeScriptChunker chunker = new TypeScriptChunker();
        
        String code = """
            export class UserService {
                constructor() {
                    this.users = [];
                }
                
                findById(id) {
                    return this.users.find(u => u.id === id);
                }
                
                async save(user) {
                    this.users.push(user);
                }
            }
            """;
        
        List<CodeChunkResult> chunks = chunker.chunk(code);
        
        System.out.println("=== TypeScript Class Chunks ===");
        System.out.println("Total chunks: " + chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            CodeChunkResult chunk = chunks.get(i);
            System.out.println("\nChunk " + (i + 1) + ":");
            System.out.println("  Name: " + chunk.chunkName());
            System.out.println("  Type: " + chunk.chunkType());
            System.out.println("  Lines: " + chunk.startLine() + "-" + chunk.endLine());
        }
    }

    @Test
    void debugPythonClass() {
        PythonChunker chunker = new PythonChunker();
        
        String code = """
            class UserService:
                def __init__(self):
                    self.users = []
                
                def find_by_id(self, user_id):
                    return next((u for u in self.users if u.id == user_id), None)
                
                async def save(self, user):
                    self.users.append(user)
            """;
        
        List<CodeChunkResult> chunks = chunker.chunk(code);
        
        System.out.println("=== Python Class Chunks ===");
        System.out.println("Total chunks: " + chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            CodeChunkResult chunk = chunks.get(i);
            System.out.println("\nChunk " + (i + 1) + ":");
            System.out.println("  Name: " + chunk.chunkName());
            System.out.println("  Type: " + chunk.chunkType());
            System.out.println("  Lines: " + chunk.startLine() + "-" + chunk.endLine());
        }
    }

    @Test
    void debugJavaClass() {
        JavaChunker chunker = new JavaChunker();
        
        String code = """
            public class UserService {
                private List<User> users;
                
                public UserService() {
                    this.users = new ArrayList<>();
                }
                
                public User findById(Long id) {
                    return users.stream()
                        .filter(u -> u.getId().equals(id))
                        .findFirst()
                        .orElse(null);
                }
                
                public void save(User user) {
                    users.add(user);
                }
            }
            """;
        
        List<CodeChunkResult> chunks = chunker.chunk(code);
        
        System.out.println("=== Java Class Chunks ===");
        System.out.println("Total chunks: " + chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            CodeChunkResult chunk = chunks.get(i);
            System.out.println("\nChunk " + (i + 1) + ":");
            System.out.println("  Name: " + chunk.chunkName());
            System.out.println("  Type: " + chunk.chunkType());
            System.out.println("  Lines: " + chunk.startLine() + "-" + chunk.endLine());
        }
    }
}
