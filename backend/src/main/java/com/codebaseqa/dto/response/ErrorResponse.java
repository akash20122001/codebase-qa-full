package com.codebaseqa.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response format for all API errors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * Error code for programmatic handling (e.g., "RESOURCE_NOT_FOUND", "RATE_LIMIT_EXCEEDED")
     */
    private String code;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * Timestamp when the error occurred
     */
    private Instant timestamp;
    
    /**
     * Request path that caused the error
     */
    private String path;
    
    /**
     * Additional error details (optional)
     */
    private Map<String, Object> details;
}
