package com.codebaseqa.service;

import com.codebaseqa.exception.ResourceNotFoundException;
import com.codebaseqa.model.CodeChunk;
import com.codebaseqa.model.IndexingJob;
import com.codebaseqa.model.Repo;
import com.codebaseqa.repository.ChunkRepository;
import com.codebaseqa.repository.IndexingJobRepository;
import com.codebaseqa.repository.RepoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {

    private final RepoRepository repoRepository;
    private final IndexingJobRepository jobRepository;
    private final ChunkRepository chunkRepository;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final CacheService cacheService;

    @Value("${app.indexing.temp-dir}")
    private String tempDir;

    @Value("${app.indexing.max-file-size-kb}")
    private int maxFileSizeKb;

    @Value("${app.indexing.supported-extensions}")
    private String supportedExtensions;

    @Value("${app.indexing.excluded-dirs}")
    private String excludedDirs;

    @Value("${app.embedding.provider:gemini}")
    private String embeddingProvider;

    private static final int EMBEDDING_BATCH_SIZE = 10; // Gemini counts each item as 1 request
    private static final long BATCH_DELAY_MS = 15000; // 15 seconds between batches (only for Gemini)

    /**
     * Process a full indexing job.
     * Called by the SQS worker.
     */
    @Transactional
    public void processFullIndexing(UUID jobId, UUID repoId) {
        IndexingJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("IndexingJob", jobId.toString()));
        Repo repo = repoRepository.findById(repoId)
            .orElseThrow(() -> new ResourceNotFoundException("Repository", repoId.toString()));

        Path cloneDir = Path.of(tempDir, repo.getId().toString());

        try {
            // Update status
            job.setStatus(IndexingJob.JobStatus.PROCESSING);
            job.setStartedAt(Instant.now());
            job.setAttempts((job.getAttempts() != null ? job.getAttempts() : 0) + 1);
            jobRepository.save(job);

            repo.setStatus(Repo.RepoStatus.INDEXING);
            repoRepository.save(repo);

            // 1. Delete existing chunks for this repo
            log.info("Deleting existing chunks for repo: {}", repo.getFullName());
            chunkRepository.deleteByRepoId(repoId);

            // 2. Clone the repository
            log.info("Cloning repo: {}", repo.getFullName());
            cloneRepository(repo, cloneDir);

            // 3. Walk files and collect supported files
            log.info("Collecting files from: {}", repo.getFullName());
            List<Path> files = collectFiles(cloneDir);
            job.setTotalFiles(files.size());
            jobRepository.save(job);

            log.info("Found {} files to index in {}", files.size(), repo.getFullName());

            // 4. Parse, chunk, embed, and store
            List<CodeChunk> allChunks = new ArrayList<>();
            int processedCount = 0;

            for (Path file : files) {
                try {
                    String content = Files.readString(file);
                    String relativePath = cloneDir.relativize(file).toString().replace("\\", "/");
                    String language = chunkingService.detectLanguage(relativePath);

                    List<ChunkingService.CodeChunkResult> fileChunks =
                        chunkingService.chunkFile(content, relativePath, language);

                    for (ChunkingService.CodeChunkResult chunk : fileChunks) {
                        CodeChunk.ChunkType chunkType;
                        try {
                            chunkType = CodeChunk.ChunkType.valueOf(chunk.chunkType());
                        } catch (IllegalArgumentException e) {
                            chunkType = CodeChunk.ChunkType.BLOCK;
                        }

                        allChunks.add(CodeChunk.builder()
                            .repo(repo)
                            .filePath(relativePath)
                            .startLine(chunk.startLine())
                            .endLine(chunk.endLine())
                            .chunkType(chunkType)
                            .chunkName(chunk.chunkName())
                            .content(chunk.content())
                            .language(language)
                            .tokenCount(chunk.content().length() / 4)
                            .build());
                    }
                } catch (Exception e) {
                    log.warn("Failed to process file: {}", file, e);
                }

                processedCount++;
                if (processedCount % 10 == 0) {
                    job.setProcessedFiles(processedCount);
                    job.setProgress((processedCount * 100) / files.size());
                    jobRepository.save(job);
                }
            }

            // Final progress update
            job.setProcessedFiles(processedCount);
            job.setProgress(100);
            jobRepository.save(job);

            log.info("Parsed {} files into {} chunks for {}", files.size(), allChunks.size(), repo.getFullName());

            // 5. Batch embed all chunks
            if (!allChunks.isEmpty()) {
                log.info("Embedding {} chunks for {}", allChunks.size(), repo.getFullName());
                embedChunksInBatches(allChunks);

                // 6. Save all chunks to database using custom insert with vector casting
                log.info("Saving {} chunks to database with vector casting", allChunks.size());
                for (CodeChunk chunk : allChunks) {
                    chunkRepository.insertWithVectorCast(
                        UUID.randomUUID(),
                        repo.getId(),
                        chunk.getFilePath(),
                        chunk.getStartLine(),
                        chunk.getEndLine(),
                        chunk.getChunkType().name(),
                        chunk.getChunkName(),
                        chunk.getContent(),
                        chunk.getLanguage(),
                        chunk.getEmbedding(),
                        chunk.getTokenCount(),
                        java.time.Instant.now()
                    );
                }
            }

            // 7. Update job and repo status
            job.setStatus(IndexingJob.JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

            repo.setStatus(Repo.RepoStatus.READY);
            repo.setLastIndexedAt(Instant.now());
            repo.setTotalChunks(allChunks.size());
            repoRepository.save(repo);

            // 8. Invalidate cache for this repo
            cacheService.invalidateRepoCache(repoId);

            log.info("✅ Indexing completed for {}: {} chunks", repo.getFullName(), allChunks.size());

        } catch (Exception e) {
            log.error("❌ Indexing failed for repo {}: {}", repo.getFullName(), e.getMessage(), e);
            job.setStatus(IndexingJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

            repo.setStatus(Repo.RepoStatus.FAILED);
            repoRepository.save(repo);

            throw new RuntimeException("Indexing failed", e);
        } finally {
            // Cleanup cloned repo
            log.info("Cleaning up cloned directory: {}", cloneDir);
            deleteDirectory(cloneDir);
        }
    }

    /**
     * Process incremental indexing (only changed files).
     */
    @Transactional
    public void processIncrementalIndexing(UUID jobId, UUID repoId, List<String> changedFiles) {
        IndexingJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        Repo repo = repoRepository.findById(repoId)
            .orElseThrow(() -> new RuntimeException("Repo not found: " + repoId));
        Path cloneDir = Path.of(tempDir, repo.getId().toString());

        try {
            job.setStatus(IndexingJob.JobStatus.PROCESSING);
            job.setStartedAt(Instant.now());
            jobRepository.save(job);

            log.info("Starting incremental indexing for {} files in {}", changedFiles.size(), repo.getFullName());

            // 1. Delete old chunks for changed files
            chunkRepository.deleteByRepoIdAndFilePaths(repoId, changedFiles);

            // 2. Clone repo
            cloneRepository(repo, cloneDir);

            // 3. Process only changed files
            List<CodeChunk> newChunks = new ArrayList<>();
            for (String filePath : changedFiles) {
                Path file = cloneDir.resolve(filePath);
                if (!Files.exists(file)) {
                    log.debug("File was deleted: {}", filePath);
                    continue; // File was deleted
                }

                try {
                    String content = Files.readString(file);
                    String language = chunkingService.detectLanguage(filePath);
                    List<ChunkingService.CodeChunkResult> fileChunks =
                        chunkingService.chunkFile(content, filePath, language);

                    for (ChunkingService.CodeChunkResult chunk : fileChunks) {
                        CodeChunk.ChunkType chunkType;
                        try {
                            chunkType = CodeChunk.ChunkType.valueOf(chunk.chunkType());
                        } catch (IllegalArgumentException e) {
                            chunkType = CodeChunk.ChunkType.BLOCK;
                        }

                        newChunks.add(CodeChunk.builder()
                            .repo(repo)
                            .filePath(filePath)
                            .startLine(chunk.startLine())
                            .endLine(chunk.endLine())
                            .chunkType(chunkType)
                            .chunkName(chunk.chunkName())
                            .content(chunk.content())
                            .language(language)
                            .tokenCount(chunk.content().length() / 4)
                            .build());
                    }
                } catch (Exception e) {
                    log.warn("Failed to process file: {}", filePath, e);
                }
            }

            // 4. Embed and save
            if (!newChunks.isEmpty()) {
                embedChunksInBatches(newChunks);
                
                // Save with vector casting
                for (CodeChunk chunk : newChunks) {
                    chunkRepository.insertWithVectorCast(
                        UUID.randomUUID(),
                        repo.getId(),
                        chunk.getFilePath(),
                        chunk.getStartLine(),
                        chunk.getEndLine(),
                        chunk.getChunkType().name(),
                        chunk.getChunkName(),
                        chunk.getContent(),
                        chunk.getLanguage(),
                        chunk.getEmbedding(),
                        chunk.getTokenCount(),
                        java.time.Instant.now()
                    );
                }
            }

            // 5. Update status
            job.setStatus(IndexingJob.JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

            repo.setLastIndexedAt(Instant.now());
            repo.setTotalChunks(chunkRepository.countByRepoId(repoId));
            repoRepository.save(repo);

            cacheService.invalidateRepoCache(repoId);

            log.info("✅ Incremental indexing completed: {} files, {} new chunks",
                changedFiles.size(), newChunks.size());

        } catch (Exception e) {
            log.error("❌ Incremental indexing failed: {}", e.getMessage(), e);
            job.setStatus(IndexingJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            jobRepository.save(job);
            throw new RuntimeException("Incremental indexing failed", e);
        } finally {
            deleteDirectory(cloneDir);
        }
    }

    private void cloneRepository(Repo repo, Path targetDir) throws Exception {
        deleteDirectory(targetDir); // Clean up any previous clone

        Files.createDirectories(targetDir.getParent());

        Git.cloneRepository()
            .setURI("https://github.com/" + repo.getFullName() + ".git")
            .setDirectory(targetDir.toFile())
            .setBranch(repo.getDefaultBranch())
            .setDepth(1)  // Shallow clone for speed
            .setCredentialsProvider(
                new UsernamePasswordCredentialsProvider(repo.getUser().getGithubToken(), ""))
            .call()
            .close();

        log.debug("Successfully cloned {} to {}", repo.getFullName(), targetDir);
    }

    private List<Path> collectFiles(Path rootDir) throws IOException {
        Set<String> extensions = Set.of(supportedExtensions.split(","));
        Set<String> excluded = Set.of(excludedDirs.split(","));
        List<Path> files = new ArrayList<>();

        Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName().toString();
                if (excluded.contains(dirName) || dirName.startsWith(".")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString();
                String ext = fileName.contains(".")
                    ? "." + fileName.substring(fileName.lastIndexOf('.') + 1)
                    : "";

                if (extensions.contains(ext) && attrs.size() <= maxFileSizeKb * 1024L) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return files;
    }

    private void embedChunksInBatches(List<CodeChunk> chunks) {
        log.info("Starting to embed {} chunks in batches of {}", chunks.size(), EMBEDDING_BATCH_SIZE);
        int totalBatches = (int) Math.ceil((double) chunks.size() / EMBEDDING_BATCH_SIZE);
        
        for (int i = 0; i < chunks.size(); i += EMBEDDING_BATCH_SIZE) {
            int batchNum = (i / EMBEDDING_BATCH_SIZE) + 1;
            int end = Math.min(i + EMBEDDING_BATCH_SIZE, chunks.size());
            List<CodeChunk> batch = chunks.subList(i, end);

            log.info("Processing batch {}/{}: chunks {}-{} of {}", 
                batchNum, totalBatches, i + 1, end, chunks.size());

            List<String> texts = batch.stream()
                .map(c -> c.getFilePath() + "\n" + c.getContent())
                .toList();

            // This now makes 1 API call per batch instead of N calls
            List<float[]> embeddings = embeddingService.embedBatch(texts);

            for (int j = 0; j < batch.size(); j++) {
                batch.get(j).setEmbedding(embeddingService.toVectorString(embeddings.get(j)));
            }

            log.info("Completed batch {}/{}: embedded {} chunks", batchNum, totalBatches, batch.size());
            
            // CRITICAL: Only Gemini needs delays due to strict rate limits
            // Voyage AI has 2,000 RPM and true batch processing - no delay needed
            if (end < chunks.size() && "gemini".equals(embeddingProvider)) {
                try {
                    log.info("Waiting {} seconds before next batch (Gemini rate limit protection)", BATCH_DELAY_MS / 1000);
                    Thread.sleep(BATCH_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during batch delay", e);
                }
            }
        }
        
        log.info("Finished embedding all {} chunks in {} API calls", chunks.size(), totalBatches);
    }

    private void deleteDirectory(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (IOException e) {
            log.warn("Failed to delete directory: {}", dir, e);
        }
    }
}
