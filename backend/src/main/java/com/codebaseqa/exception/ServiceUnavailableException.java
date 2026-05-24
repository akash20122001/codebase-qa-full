package com.codebaseqa.exception;

/**
 * Exception thrown when an external service (LLM, Embedding, GitHub) is unavailable.
 * Results in HTTP 503 Service Unavailable.
 */
public class ServiceUnavailableException extends RuntimeException {
    
    private final String serviceName;
    
    public ServiceUnavailableException(String serviceName, String message) {
        super(String.format("%s service is unavailable: %s", serviceName, message));
        this.serviceName = serviceName;
    }
    
    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super(String.format("%s service is unavailable", serviceName), cause);
        this.serviceName = serviceName;
    }
    
    public String getServiceName() {
        return serviceName;
    }
}
