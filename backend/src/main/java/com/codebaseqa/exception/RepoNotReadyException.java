package com.codebaseqa.exception;

import com.codebaseqa.model.Repo;

/**
 * Exception thrown when attempting to query a repository that's not ready.
 * Results in HTTP 400 Bad Request.
 */
public class RepoNotReadyException extends RuntimeException {
    
    private final Repo.RepoStatus currentStatus;
    
    public RepoNotReadyException(Repo.RepoStatus currentStatus) {
        super(String.format("Repository is not ready for queries. Current status: %s", currentStatus));
        this.currentStatus = currentStatus;
    }
    
    public Repo.RepoStatus getCurrentStatus() {
        return currentStatus;
    }
}
