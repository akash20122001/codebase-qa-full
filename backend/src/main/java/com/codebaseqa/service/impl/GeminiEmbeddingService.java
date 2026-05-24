package com.codebaseqa.service.impl;

import com.codebaseqa.service.EmbeddingService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "gemini", matchIfMissing = true)
@Slf4j
public class GeminiEmbeddingService implements EmbeddingService {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.embedding-model}")
    private String embeddingModel;

    @Value("${app.gemini.rate-limit.delay-ms:1000}")
    private long rateLimitDelayMs;

    @Value("${app.gemini.rate-limit.max-retries:5}")
    private int maxRetries;

    public GeminiEmbeddingService(WebClient.Builder webClientBuilder, CircuitBreakerRegistry registry) {
        this.webClient = webClientBuilder
            .baseUrl("https://generativelanguage.googleapis.com")
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024)) // 10MB buffer for large batch responses
            .build();
        this.circuitBreaker = registry.circuitBreaker("gemini-embedding");
    }

    @Override
    public float[] embedText(String text) {
        return embedTextWithRetry(text, 0);
    }

    private float[] embedTextWithRetry(String text, int attempt) {
        try {
            log.debug("Embedding text of length: {} (attempt {})", text.length(), attempt + 1);
            
            Map<String, Object> response = webClient.post()
                .uri("/v1beta/models/{model}:embedContent?key={key}", embeddingModel, apiKey)
                .bodyValue(Map.of(
                    "model", "models/" + embeddingModel,
                    "content", Map.of("parts", List.of(Map.of("text", text)))
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response == null) {
                throw new RuntimeException("Null response from Gemini API");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> embedding = (Map<String, Object>) response.get("embedding");
            if (embedding == null) {
                throw new RuntimeException("No embedding in response");
            }

            @SuppressWarnings("unchecked")
            List<Double> values = (List<Double>) embedding.get("values");
            if (values == null || values.isEmpty()) {
                throw new RuntimeException("No values in embedding");
            }

            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i).floatValue();
            }
            
            log.debug("Successfully embedded text, dimension: {}", result.length);
            return result;
            
        } catch (WebClientResponseException.TooManyRequests e) {
            if (attempt >= maxRetries) {
                log.error("Max retries ({}) exceeded for rate limit", maxRetries);
                throw new RuntimeException("Rate limit exceeded after " + maxRetries + " retries", e);
            }
            
            // Exponential backoff: 2^attempt * base delay
            long backoffMs = (long) (Math.pow(2, attempt) * rateLimitDelayMs);
            log.warn("Rate limit hit (429), retrying in {}ms (attempt {}/{})", 
                backoffMs, attempt + 1, maxRetries);
            
            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during backoff", ie);
            }
            
            return embedTextWithRetry(text, attempt + 1);
            
        } catch (Exception e) {
            log.error("Error embedding text: {}", e.getMessage());
            throw new RuntimeException("Failed to embed text", e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        return embedBatchWithRetry(texts, 0);
    }

    private List<float[]> embedBatchWithRetry(List<String> texts, int attempt) {
        try {
            log.info("Batch embedding {} texts using batchEmbedContents API (attempt {})", 
                texts.size(), attempt + 1);
            
            // Build requests array for batch API
            List<Map<String, Object>> requests = texts.stream()
                .map(text -> Map.of(
                    "model", "models/" + embeddingModel,
                    "content", Map.of("parts", List.of(Map.of("text", (Object) text)))
                ))
                .map(m -> (Map<String, Object>) m)
                .toList();

            // Call the batch embedding API
            Map<String, Object> response = webClient.post()
                .uri("/v1beta/models/{model}:batchEmbedContents?key={key}", embeddingModel, apiKey)
                .bodyValue(Map.of("requests", requests))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response == null) {
                throw new RuntimeException("Null response from Gemini batch API");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> embeddings = (List<Map<String, Object>>) response.get("embeddings");
            if (embeddings == null || embeddings.isEmpty()) {
                throw new RuntimeException("No embeddings in batch response");
            }

            if (embeddings.size() != texts.size()) {
                throw new RuntimeException(String.format(
                    "Mismatch: requested %d embeddings but got %d", texts.size(), embeddings.size()));
            }

            // Extract float arrays from response
            List<float[]> results = new java.util.ArrayList<>();
            for (Map<String, Object> embeddingObj : embeddings) {
                @SuppressWarnings("unchecked")
                List<Double> values = (List<Double>) embeddingObj.get("values");
                if (values == null || values.isEmpty()) {
                    throw new RuntimeException("No values in embedding");
                }

                float[] result = new float[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    result[i] = values.get(i).floatValue();
                }
                results.add(result);
            }

            log.info("Successfully batch embedded {} texts in 1 API call", results.size());
            return results;
            
        } catch (WebClientResponseException.TooManyRequests e) {
            if (attempt >= maxRetries) {
                log.error("Max retries ({}) exceeded for batch embedding rate limit", maxRetries);
                throw new RuntimeException("Rate limit exceeded after " + maxRetries + " retries", e);
            }
            
            // Exponential backoff with jitter (Gemini recommendation)
            // Base: 2^attempt * 1.5s, Jitter: +0-500ms randomization
            long baseBackoff = (long) (Math.pow(2, attempt) * rateLimitDelayMs);
            long jitter = (long) (Math.random() * 500); // 0-500ms random jitter
            long backoffMs = baseBackoff + jitter;
            
            log.warn("Batch embedding rate limit hit (429), retrying in {}ms (attempt {}/{})", 
                backoffMs, attempt + 1, maxRetries);
            
            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during backoff", ie);
            }
            
            return embedBatchWithRetry(texts, attempt + 1);
            
        } catch (Exception e) {
            log.error("Error batch embedding texts: {}", e.getMessage());
            throw new RuntimeException("Failed to batch embed texts", e);
        }
    }

    @Override
    public String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int getDimension() {
        return 3072; // gemini-embedding-001 outputs 3072 dimensions
    }
}
