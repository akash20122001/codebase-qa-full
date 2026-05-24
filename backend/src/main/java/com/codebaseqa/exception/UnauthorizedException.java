package com.codebaseqa.exception;

/**
 * Exception thrown when a user attempts to access a resource they don't own.
 * Results in HTTP 403 Forbidden.
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String resourceType, String action) {
        super(String.format("You don't have permission to %s this %s", action, resourceType));
    }
}
