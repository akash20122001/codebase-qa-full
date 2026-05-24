package com.codebaseqa.service.chunking;

import com.codebaseqa.service.ChunkingService.CodeChunkResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for improved chunking logic across TypeScript, Python, and Java.
 */
class ImprovedChunkingTest {

    @Test
    void testTypeScriptArrowFunctions() {
        TypeScriptChunker chunker = new TypeScriptChunker();
        
        String code = """
            const hello = () => {
                console.log('hello');
            };
            
            export const greet = async (name) => {
                return `Hello, ${name}`;
            };
            """;
        
        List<CodeChunkResult> chunks = chunker.chunk(code);
        
        assertEquals(2, chunks.size(), "Should extract 2 arrow functions");
        assertEquals("hello", chunks.get(0).chunkName());
        assertEquals("FUNCTION", chunks.get(0).chunkType());
        assertEquals("greet", chunks.get(1).chunkName());
        assertEquals("FUNCTION", chunks.get(1).chunkType());
    }

    @Test
    void testTypeScriptClassWithMethods() {
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
        
        assertTrue(chunks.size() >= 3, "Should extract class and methods");
        
        // Find class chunk
        CodeChunkResult classChunk = chunks.stream()
            .filter(c -> "CLASS".equals(c.chunkType()))
            .findFirst()
            .orElse(null);
        assertNotNull(classChunk);
        assertEquals("UserService", classChunk.chunkName());
        
        // Find method chunks
        List<CodeChunkResult> methods = chunks.stream()
            .filter(c -> "METHOD".equals(c.chunkType()))
            .toList();
        assertTrue(methods.size() >= 2, "Should extract at least 2 methods");
        assertTrue(methods.stream().anyMatch(m -> "UserService.findById".equals(m.chunkName())));
        assertTrue(methods.stream().anyMatch(m -> "UserService.save".equals(m.chunkName())));
    }

    @Test
    void testPythonClassWithMethods() {
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
        
        assertTrue(chunks.size() >= 3, "Should extract class and methods");
        
        // Find class chunk
        CodeChunkResult classChunk = chunks.stream()
            .filter(c -> "CLASS".equals(c.chunkType()))
            .findFirst()
            .orElse(null);
        assertNotNull(classChunk);
        assertEquals("UserService", classChunk.chunkName());
        
        // Find method chunks
        List<CodeChunkResult> methods = chunks.stream()
            .filter(c -> "METHOD".equals(c.chunkType()))
            .toList();
        assertTrue(methods.size() >= 2, "Should extract at least 2 methods (excluding __init__)");
        assertTrue(methods.stream().anyMatch(m -> m.chunkName().contains("find_by_id")));
        assertTrue(methods.stream().anyMatch(m -> m.chunkName().contains("save")));
    }

    @Test
    void testPythonDecorators() {
        PythonChunker chunker = new PythonChunker();
        
        String code = """
            @app.route('/users')
            @login_required
            def get_users():
                return User.query.all()
            
            @staticmethod
            def validate(data):
                return data is not None
            """;
        
        List<CodeChunkResult> chunks = chunker.chunk(code);
        
        assertEquals(2, chunks.size(), "Should extract 2 functions with decorators");
        
        // Check that decorators are included in chunks
        assertTrue(chunks.get(0).content().contains("@app.route"));
        assertTrue(chunks.get(0).content().contains("@login_required"));
        assertEquals("get_users", chunks.get(0).chunkName());
        
        assertTrue(chunks.get(1).content().contains("@staticmethod"));
        assertEquals("validate", chunks.get(1).chunkName());
    }

    @Test
    void testJavaClassWithMethods() {
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
        
        assertTrue(chunks.size() >= 3, "Should extract class, constructor, and methods");
        
        // Find class chunk
        CodeChunkResult classChunk = chunks.stream()
            .filter(c -> "CLASS".equals(c.chunkType()))
            .findFirst()
            .orElse(null);
        assertNotNull(classChunk);
        assertEquals("UserService", classChunk.chunkName());
        
        // Find constructor
        CodeChunkResult constructor = chunks.stream()
            .filter(c -> "CONSTRUCTOR".equals(c.chunkType()))
            .findFirst()
            .orElse(null);
        assertNotNull(constructor);
        assertEquals("UserService.<init>", constructor.chunkName());
        
        // Find methods
        List<CodeChunkResult> methods = chunks.stream()
            .filter(c -> "METHOD".equals(c.chunkType()))
            .toList();
        assertTrue(methods.size() >= 2, "Should extract at least 2 methods");
        assertTrue(methods.stream().anyMatch(m -> "UserService.findById".equals(m.chunkName())));
        assertTrue(methods.stream().anyMatch(m -> "UserService.save".equals(m.chunkName())));
    }

    @Test
    void testJavaInterface() {
        JavaChunker chunker = new JavaChunker();
        
        String code = """
            public interface UserRepository {
                User findById(Long id);
                void save(User user);
                List<User> findAll();
            }
            """;
        
        List<CodeChunkResult> chunks = chunker.chunk(code);
        
        assertTrue(chunks.size() >= 1, "Should extract interface");
        assertEquals("INTERFACE", chunks.get(0).chunkType());
        assertEquals("UserRepository", chunks.get(0).chunkName());
    }

    @Test
    void testJavaEnum() {
        JavaChunker chunker = new JavaChunker();
        
        String code = """
            public enum Status {
                ACTIVE,
                INACTIVE,
                PENDING
            }
            """;
        
        List<CodeChunkResult> chunks = chunker.chunk(code);
        
        assertEquals(1, chunks.size(), "Should extract enum");
        assertEquals("ENUM", chunks.get(0).chunkType());
        assertEquals("Status", chunks.get(0).chunkName());
    }
}
