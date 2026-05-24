package com.codebaseqa.service;

import com.codebaseqa.dto.request.ConnectRepoRequest;
import com.codebaseqa.exception.ResourceNotFoundException;
import com.codebaseqa.model.IndexingJob;
import com.codebaseqa.model.Repo;
import com.codebaseqa.model.User;
import com.codebaseqa.repository.IndexingJobRepository;
import com.codebaseqa.repository.RepoRepository;
import com.codebaseqa.util.GitHubClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepoService {

    private final RepoRepository repoRepository;
    private final IndexingJobRepository jobRepository;
    private final GitHubClient gitHubClient;
    private final SqsService sqsService;

    /**
     * Connect a GitHub repository and trigger indexing.
     * 1. Verify user has access to the repo on GitHub
     * 2. Check if already connected
     * 3. Save repo to database
     * 4. Create indexing job
     * 5. Send message to SQS queue (after transaction commits)
     */
    public ConnectRepoResult connectRepo(ConnectRepoRequest request, User user) {
        // Create repo and job in a transaction
        ConnectRepoResult result = createRepoAndJob(request, user);
        
        // Send to SQS AFTER transaction commits
        sqsService.sendIndexingMessage(result.jobId(), result.repo().getId());
        
        log.info("Repo connected and indexing queued: {} (jobId={})", 
            result.repo().getFullName(), result.jobId());
        return result;
    }

    @Transactional
    private ConnectRepoResult createRepoAndJob(ConnectRepoRequest request, User user) {
        // 1. Verify user has access to this repo on GitHub
        var ghRepo = gitHubClient.getRepository(request.getRepoFullName(), user.getGithubToken());

        // 2. Check if already connected
        repoRepository.findByUserIdAndGithubRepoId(user.getId(), ghRepo.id())
            .ifPresent(r -> {
                throw new IllegalStateException("Repository already connected");
            });

        // 3. Save repo
        Repo repo = repoRepository.save(Repo.builder()
            .user(user)
            .githubRepoId(ghRepo.id())
            .fullName(request.getRepoFullName())
            .defaultBranch(request.getBranch() != null ? request.getBranch() : ghRepo.defaultBranch())
            .status(Repo.RepoStatus.PENDING)
            .build());

        // 4. Create indexing job
        IndexingJob job = jobRepository.save(IndexingJob.builder()
            .repo(repo)
            .status(IndexingJob.JobStatus.QUEUED)
            .jobType(IndexingJob.JobType.FULL)
            .build());

        return new ConnectRepoResult(repo, job.getId());
    }

    public record ConnectRepoResult(Repo repo, UUID jobId) {}

    /**
     * Get all repositories for a user.
     */
    public List<Repo> getUserRepos(UUID userId) {
        return repoRepository.findByUserId(userId);
    }

    /**
     * Get a specific repository by ID.
     * Verifies that the repo belongs to the user.
     */
    public Repo getRepo(UUID repoId, UUID userId) {
        return repoRepository.findByIdAndUserId(repoId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Repository", repoId.toString()));
    }

    /**
     * Disconnect a repository.
     * Removes webhook from GitHub and deletes the repo from database.
     * Cascade delete handles chunks, conversations, and jobs.
     */
    @Transactional
    public void disconnectRepo(UUID repoId, UUID userId) {
        Repo repo = getRepo(repoId, userId);

        // Remove webhook from GitHub if exists
        if (repo.getWebhookId() != null) {
            gitHubClient.deleteWebhook(repo.getFullName(), repo.getWebhookId(),
                repo.getUser().getGithubToken());
        }

        // CASCADE delete handles chunks, conversations, jobs
        repoRepository.delete(repo);
        log.info("Repo disconnected: {}", repo.getFullName());
    }

    /**
     * Trigger a manual re-index of a repository.
     * Creates a new indexing job and sends it to SQS.
     */
    public IndexingJob triggerReindex(UUID repoId, UUID userId) {
        // Create job in transaction
        IndexingJob job = createReindexJob(repoId, userId);
        
        // Send to SQS AFTER transaction commits
        sqsService.sendIndexingMessage(job.getId(), job.getRepo().getId());
        
        log.info("Manual reindex triggered for {}: jobId={}", 
            job.getRepo().getFullName(), job.getId());
        return job;
    }

    @Transactional
    private IndexingJob createReindexJob(UUID repoId, UUID userId) {
        Repo repo = getRepo(repoId, userId);

        // Check if already indexing
        boolean alreadyIndexing = jobRepository.existsByRepoIdAndStatusIn(
            repoId, List.of(IndexingJob.JobStatus.QUEUED, IndexingJob.JobStatus.PROCESSING));
        if (alreadyIndexing) {
            throw new IllegalStateException("Indexing already in progress");
        }

        return jobRepository.save(IndexingJob.builder()
            .repo(repo)
            .status(IndexingJob.JobStatus.QUEUED)
            .jobType(IndexingJob.JobType.FULL)
            .build());
    }
}
