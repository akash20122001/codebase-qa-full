package com.codebaseqa.worker;

import com.codebaseqa.model.IndexingJob;
import com.codebaseqa.repository.IndexingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaleJobCleanup {

    private final IndexingJobRepository jobRepository;

    /**
     * Every 5 minutes, check for jobs stuck in PROCESSING for more than 10 minutes.
     * Mark them as FAILED so they can be retried.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    public void cleanupStaleJobs() {
        Instant cutoff = Instant.now().minus(10, ChronoUnit.MINUTES);
        List<IndexingJob> staleJobs = jobRepository.findStaleJobs(cutoff);

        for (IndexingJob job : staleJobs) {
            log.warn("Marking stale job as FAILED: jobId={}, repoId={}",
                job.getId(), job.getRepo().getId());
            job.setStatus(IndexingJob.JobStatus.FAILED);
            job.setErrorMessage("Job timed out (stuck in PROCESSING for >10 minutes)");
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }

        if (!staleJobs.isEmpty()) {
            log.info("Cleaned up {} stale jobs", staleJobs.size());
        }
    }
}
