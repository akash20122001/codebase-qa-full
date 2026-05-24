package com.codebaseqa.exception;

/**
 * Exception thrown for invalid request data or business logic violations.
 * Results in HTTP 400 Bad Request.
 */
public class InvalidRequestException extends RuntimeException {
    
    public InvalidRequestException(String message) {
        super(message);
    }
    
    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
