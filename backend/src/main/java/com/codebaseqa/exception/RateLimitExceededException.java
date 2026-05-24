package com.codebaseqa.exception;

/**
 * Exception thrown when a user exceeds their rate limit.
 * Results in HTTP 429 Too Many Requests.
 */
public class RateLimitExceededException extends RuntimeException {
    
    private final long retryAfterSeconds;
    
    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
