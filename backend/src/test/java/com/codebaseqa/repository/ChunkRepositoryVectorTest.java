package com.codebaseqa.repository;

import com.codebaseqa.model.CodeChunk;
import com.codebaseqa.model.Repo;
import com.codebaseqa.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify vector casting works without consuming API quota.
 * This test uses a fake embedding vector to test database insertion.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ChunkRepositoryVectorTest {

    @Autowired
    private ChunkRepository chunkRepository;

    @Autowired
    private RepoRepository repoRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testInsertChunkWithVectorCast() {
        // 1. Create a test user
        User user = User.builder()
            .githubId("999999")
            .username("testuser")
            .email("test@example.com")
            .githubToken("fake-token")
            .build();
        user = userRepository.save(user);

        // 2. Create a test repo
        Repo repo = Repo.builder()
            .user(user)
            .githubRepoId(888888L)
            .fullName("testuser/testrepo")
            .defaultBranch("main")
            .status(Repo.RepoStatus.PENDING)
            .build();
        final Repo savedRepo = repoRepository.save(repo);

        // 3. Create a fake embedding vector (3072 dimensions)
        // Just use simple values - we're testing the database, not the embedding quality
        StringBuilder embeddingBuilder = new StringBuilder("[");
        for (int i = 0; i < 3072; i++) {
            if (i > 0) embeddingBuilder.append(",");
            embeddingBuilder.append(String.format("%.6f", i * 0.001)); // Simple pattern
        }
        embeddingBuilder.append("]");
        final String embedding = embeddingBuilder.toString();

        // 4. Test the custom insert method with vector casting
        final UUID chunkId = UUID.randomUUID();
        
        assertDoesNotThrow(() -> {
            chunkRepository.insertWithVectorCast(
                chunkId,
                savedRepo.getId(),
                "test/TestFile.java",
                1,
                10,
                CodeChunk.ChunkType.FUNCTION.name(),
                "testFunction",
                "public void test() { System.out.println(\"test\"); }",
                "java",
                embedding,
                100,
                Instant.now()
            );
        }, "Vector insert should not throw an exception");

        // 5. Verify the chunk was inserted
        assertTrue(chunkRepository.existsById(chunkId), 
            "Chunk should exist in database after insert");

        // 6. Clean up
        chunkRepository.deleteById(chunkId);
        repoRepository.delete(savedRepo);
        userRepository.delete(user);

        System.out.println("✅ SUCCESS! Vector casting works correctly!");
    }

    @Test
    void testInsertMultipleChunksWithVectorCast() {
        // Test inserting multiple chunks to ensure it works in batch scenarios
        
        User user = User.builder()
            .githubId("999998")
            .username("testuser2")
            .email("test2@example.com")
            .githubToken("fake-token-2")
            .build();
        user = userRepository.save(user);

        Repo repo = Repo.builder()
            .user(user)
            .githubRepoId(888887L)
            .fullName("testuser2/testrepo2")
            .defaultBranch("main")
            .status(Repo.RepoStatus.PENDING)
            .build();
        repo = repoRepository.save(repo);

        // Create 5 test chunks
        for (int i = 0; i < 5; i++) {
            StringBuilder embedding = new StringBuilder("[");
            for (int j = 0; j < 3072; j++) {
                if (j > 0) embedding.append(",");
                embedding.append(String.format("%.6f", (i + j) * 0.001));
            }
            embedding.append("]");

            UUID chunkId = UUID.randomUUID();
            
            chunkRepository.insertWithVectorCast(
                chunkId,
                repo.getId(),
                "test/TestFile" + i + ".java",
                i * 10 + 1,
                i * 10 + 10,
                CodeChunk.ChunkType.FUNCTION.name(),
                "testFunction" + i,
                "public void test" + i + "() { }",
                "java",
                embedding.toString(),
                100,
                Instant.now()
            );
        }

        // Verify all chunks were inserted
        int count = chunkRepository.countByRepoId(repo.getId());
        assertEquals(5, count, "Should have inserted 5 chunks");

        // Clean up
        chunkRepository.deleteByRepoId(repo.getId());
        repoRepository.delete(repo);
        userRepository.delete(user);

        System.out.println("✅ SUCCESS! Multiple vector inserts work correctly!");
    }
}
