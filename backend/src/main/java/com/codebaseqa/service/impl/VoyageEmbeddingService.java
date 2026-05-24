package com.codebaseqa.service.impl;

import com.codebaseqa.service.EmbeddingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Voyage AI embedding service using voyage-code-3 model.
 * Purpose-built for code retrieval with 2,000 RPM and true batch processing.
 */
@Service
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "voyage")
@Slf4j
public class VoyageEmbeddingService implements EmbeddingService {

    private static final String API_URL = "https://api.voyageai.com/v1/embeddings";
    private static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int dimension;
    private final int batchSize;

    public VoyageEmbeddingService(
            @Value("${app.embedding.voyage.api-key}") String apiKey,
            @Value("${app.embedding.voyage.model:voyage-code-3}") String model,
            @Value("${app.embedding.voyage.dimension:1024}") int dimension,
            @Value("${app.embedding.voyage.batch-size:128}") int batchSize,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.dimension = dimension;
        this.batchSize = batchSize;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        
        log.info("🚀 Initialized Voyage AI Embedding Service");
        log.info("   Model: {}", model);
        log.info("   Dimension: {}", dimension);
        log.info("   Batch Size: {}", batchSize);
    }

    @Override
    public float[] embedText(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        log.info("Batch embedding {} texts using Voyage AI (batches of {})", texts.size(), batchSize);
        
        List<float[]> allEmbeddings = new ArrayList<>();
        int totalBatches = (int) Math.ceil((double) texts.size() / batchSize);
        
        for (int i = 0; i < texts.size(); i += batchSize) {
            int batchNum = (i / batchSize) + 1;
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            
            log.info("Processing batch {}/{}: items {}-{} of {}", 
                batchNum, totalBatches, i + 1, end, texts.size());
            
            List<float[]> batchEmbeddings = callApi(batch, "document");
            allEmbeddings.addAll(batchEmbeddings);
            
            log.info("Completed batch {}/{}: embedded {} items", batchNum, totalBatches, batch.size());
        }
        
        log.info("Successfully embedded {} texts in {} API call(s)", texts.size(), totalBatches);
        return allEmbeddings;
    }

    /**
     * Embed texts with specified input type.
     * @param texts List of texts to embed
     * @param inputType "document" for indexing, "query" for search
     */
    public List<float[]> embedBatch(List<String> texts, String inputType) {
        log.info("Batch embedding {} texts with input_type={}", texts.size(), inputType);
        
        List<float[]> allEmbeddings = new ArrayList<>();
        int totalBatches = (int) Math.ceil((double) texts.size() / batchSize);
        
        for (int i = 0; i < texts.size(); i += batchSize) {
            int batchNum = (i / batchSize) + 1;
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            
            log.info("Processing batch {}/{}: items {}-{} of {}", 
                batchNum, totalBatches, i + 1, end, texts.size());
            
            List<float[]> batchEmbeddings = callApi(batch, inputType);
            allEmbeddings.addAll(batchEmbeddings);
        }
        
        return allEmbeddings;
    }

    private List<float[]> callApi(List<String> texts, String inputType) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", texts,
                    "input_type", inputType,
                    "output_dimension", dimension
            );
            
            RequestBody requestBody = RequestBody.create(
                    objectMapper.writeValueAsBytes(body),
                    JSON);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("Voyage API error: {} - {}", response.code(), errorBody);
                    throw new RuntimeException("Voyage API error: " + response.code() + " - " + errorBody);
                }
                
                JsonNode root = objectMapper.readTree(response.body().bytes());
                List<float[]> embeddings = new ArrayList<>();
                
                JsonNode dataArray = root.get("data");
                if (dataArray == null || !dataArray.isArray()) {
                    throw new RuntimeException("Invalid response from Voyage API: missing 'data' array");
                }
                
                for (JsonNode data : dataArray) {
                    JsonNode embeddingNode = data.get("embedding");
                    if (embeddingNode == null || !embeddingNode.isArray()) {
                        throw new RuntimeException("Invalid response from Voyage API: missing 'embedding' in data");
                    }
                    
                    float[] vec = new float[dimension];
                    for (int j = 0; j < dimension; j++) {
                        vec[j] = (float) embeddingNode.get(j).asDouble();
                    }
                    embeddings.add(vec);
                }
                
                if (embeddings.size() != texts.size()) {
                    throw new RuntimeException(String.format(
                        "Mismatch: requested %d embeddings but got %d", texts.size(), embeddings.size()));
                }
                
                return embeddings;
            }
        } catch (IOException e) {
            log.error("Failed to call Voyage API", e);
            throw new RuntimeException("Failed to call Voyage API", e);
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        return sb.append("]").toString();
    }
}
