package com.codebaseqa.worker;

import com.codebaseqa.model.IndexingJob;
import com.codebaseqa.repository.IndexingJobRepository;
import com.codebaseqa.service.IndexingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class IndexingWorker {

    private final SqsClient sqsClient;
    private final IndexingService indexingService;
    private final IndexingJobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.aws.sqs.queue-url}")
    private String queueUrl;

    private static final int MAX_RETRIES = 3;

    /**
     * Poll SQS every 5 seconds for new indexing jobs.
     */
    @Scheduled(fixedDelay = 5000)
    public void pollMessages() {
        if (queueUrl == null || queueUrl.isEmpty()) {
            log.debug("SQS queue URL not configured, skipping poll");
            return;
        }

        try {
            ReceiveMessageResponse response = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1)       // Process one at a time
                    .waitTimeSeconds(5)           // Long polling (reduces API calls)
                    .visibilityTimeout(600)       // 10 min to process before retry
                    .build()
            );

            for (Message message : response.messages()) {
                processMessage(message);
            }
        } catch (Exception e) {
            log.error("Error polling SQS", e);
        }
    }

    private void processMessage(Message message) {
        try {
            JsonNode body = objectMapper.readTree(message.body());
            UUID jobId = UUID.fromString(body.get("jobId").asText());
            UUID repoId = UUID.fromString(body.get("repoId").asText());

            log.info("Processing indexing job: jobId={}, repoId={}", jobId, repoId);

            // Check if job should be retried
            IndexingJob job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                log.warn("Job not found, deleting message: {}", jobId);
                deleteMessage(message);
                return;
            }

            // Handle null attempts (initialize to 0 if null)
            int attempts = job.getAttempts() != null ? job.getAttempts() : 0;
            
            if (attempts >= MAX_RETRIES) {
                log.error("Job exceeded max retries, moving to DLQ: {}", jobId);
                job.setStatus(IndexingJob.JobStatus.FAILED);
                job.setErrorMessage("Exceeded maximum retry attempts (" + MAX_RETRIES + ")");
                jobRepository.save(job);
                deleteMessage(message);
                return;
            }

            // Check if it's incremental or full
            if (job.getJobType() == IndexingJob.JobType.INCREMENTAL
                && job.getChangedFiles() != null) {
                indexingService.processIncrementalIndexing(jobId, repoId, job.getChangedFiles());
            } else {
                indexingService.processFullIndexing(jobId, repoId);
            }

            // Success — delete message from queue
            deleteMessage(message);
            log.info("✅ Indexing job completed successfully: {}", jobId);

        } catch (Exception e) {
            log.error("❌ Failed to process indexing message: {}", e.getMessage(), e);
            // Don't delete message — it will become visible again after visibility timeout
            // and be retried (or moved to DLQ by SQS after maxReceiveCount)
        }
    }

    private void deleteMessage(Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(message.receiptHandle())
            .build());
    }
}
