package com.codebaseqa.service;

import com.codebaseqa.exception.InvalidRequestException;
import com.codebaseqa.exception.ResourceNotFoundException;
import com.codebaseqa.exception.UnauthorizedException;
import com.codebaseqa.model.IndexingJob;
import com.codebaseqa.model.Repo;
import com.codebaseqa.repository.IndexingJobRepository;
import com.codebaseqa.repository.RepoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling GitHub webhook events.
 * Verifies webhook signatures and triggers incremental re-indexing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final RepoRepository repoRepository;
    private final IndexingJobRepository jobRepository;
    private final SqsService sqsService;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    @Value("${app.github.webhook-secret:}")
    private String webhookSecret;

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Process a GitHub push event webhook.
     * Verifies signature, extracts changed files, and triggers incremental indexing.
     *
     * @param signature GitHub signature header
     * @param payload   Webhook payload
     * @return Processing result
     */
    @Transactional
    public Map<String, Object> processGitHubPushEvent(String signature, Map<String, Object> payload) {
        log.info("Processing GitHub push event");

        // Verify signature if webhook secret is configured
        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            verifySignature(signature, payload);
        } else {
            log.warn("Webhook secret not configured - skipping signature verification");
        }

        // Extract repository information
        Map<String, Object> repository = (Map<String, Object>) payload.get("repository");
        if (repository == null) {
            throw new InvalidRequestException("Missing repository information in webhook payload");
        }

        String fullName = (String) repository.get("full_name");
        if (fullName == null) {
            throw new InvalidRequestException("Missing repository full_name in webhook payload");
        }

        log.info("Webhook for repository: {}", fullName);

        // Find the repository in our database
        Repo repo = repoRepository.findByFullName(fullName)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", fullName));

        // Extract ref (branch)
        String ref = (String) payload.get("ref");
        String branch = ref != null ? ref.replace("refs/heads/", "") : null;

        log.info("Push to branch: {}", branch);

        // Only process if it's the branch we're tracking
        if (branch != null && !branch.equals(repo.getDefaultBranch())) {
            log.info("Ignoring push to branch {} (tracking {})", branch, repo.getDefaultBranch());
            return Map.of(
                    "status", "ignored",
                    "message", "Push to non-tracked branch",
                    "branch", branch,
                    "trackedBranch", repo.getDefaultBranch()
            );
        }

        // Extract changed files from commits
        List<String> changedFiles = extractChangedFiles(payload);

        if (changedFiles.isEmpty()) {
            log.info("No relevant files changed in this push");
            return Map.of(
                    "status", "skipped",
                    "message", "No code files changed"
            );
        }

        log.info("Changed files: {}", changedFiles);

        // Create incremental indexing job
        IndexingJob job = IndexingJob.builder()
                .repo(repo)
                .status(IndexingJob.JobStatus.QUEUED)
                .jobType(IndexingJob.JobType.INCREMENTAL)
                .build();
        job = jobRepository.save(job);

        // Send to SQS for processing
        sqsService.sendIncrementalIndexingMessage(job.getId(), repo.getId(), changedFiles);

        // Invalidate cache for this repository
        cacheService.invalidateRepoCache(repo.getId());

        log.info("Incremental indexing job {} queued for {} files", job.getId(), changedFiles.size());

        return Map.of(
                "status", "queued",
                "message", "Incremental indexing job created",
                "jobId", job.getId().toString(),
                "filesChanged", changedFiles.size(),
                "files", changedFiles
        );
    }

    /**
     * Verify GitHub webhook signature using HMAC SHA-256.
     *
     * @param signature Signature from X-Hub-Signature-256 header
     * @param payload   Webhook payload
     * @throws UnauthorizedException if signature is invalid
     */
    private void verifySignature(String signature, Map<String, Object> payload) {
        if (signature == null || !signature.startsWith("sha256=")) {
            throw new UnauthorizedException("Invalid or missing webhook signature");
        }

        try {
            // Convert payload to JSON string
            String payloadJson = objectMapper.writeValueAsString(payload);

            // Calculate expected signature
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKey);

            byte[] hash = mac.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "sha256=" + bytesToHex(hash);

            // Compare signatures (constant-time comparison)
            if (!constantTimeEquals(signature, expectedSignature)) {
                log.error("Webhook signature verification failed");
                log.error("Received: {}", signature);
                log.error("Expected: {}", expectedSignature);
                throw new UnauthorizedException("Webhook signature verification failed");
            }

            log.info("Webhook signature verified successfully");

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying webhook signature", e);
            throw new RuntimeException("Failed to verify webhook signature", e);
        } catch (Exception e) {
            log.error("Error processing webhook signature", e);
            throw new UnauthorizedException("Invalid webhook signature");
        }
    }

    /**
     * Extract changed files from webhook payload commits.
     *
     * @param payload Webhook payload
     * @return List of changed file paths
     */
    private List<String> extractChangedFiles(Map<String, Object> payload) {
        List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");
        if (commits == null || commits.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> changedFiles = new HashSet<>();

        for (Map<String, Object> commit : commits) {
            // Added files
            List<String> added = (List<String>) commit.get("added");
            if (added != null) {
                changedFiles.addAll(added);
            }

            // Modified files
            List<String> modified = (List<String>) commit.get("modified");
            if (modified != null) {
                changedFiles.addAll(modified);
            }

            // Removed files (we'll delete their chunks)
            List<String> removed = (List<String>) commit.get("removed");
            if (removed != null) {
                changedFiles.addAll(removed);
            }
        }

        // Filter to only include code files (exclude docs, configs, etc.)
        return changedFiles.stream()
                .filter(this::isCodeFile)
                .collect(Collectors.toList());
    }

    /**
     * Check if a file is a code file that should be indexed.
     *
     * @param filePath File path
     * @return true if it's a code file
     */
    private boolean isCodeFile(String filePath) {
        String lowerPath = filePath.toLowerCase();

        // Supported code file extensions
        String[] codeExtensions = {
                ".java", ".ts", ".tsx", ".js", ".jsx", ".py", ".go", ".rs",
                ".rb", ".php", ".cs", ".kt", ".swift", ".scala", ".cpp", ".cc",
                ".cxx", ".c", ".h", ".hpp"
        };

        for (String ext : codeExtensions) {
            if (lowerPath.endsWith(ext)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Convert byte array to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
