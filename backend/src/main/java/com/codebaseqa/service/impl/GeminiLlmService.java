package com.codebaseqa.service.impl;

import com.codebaseqa.exception.ServiceUnavailableException;
import com.codebaseqa.service.LlmService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class GeminiLlmService implements LlmService {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.chat-model}")
    private String chatModel;

    public GeminiLlmService(WebClient.Builder webClientBuilder, CircuitBreakerRegistry registry) {
        this.webClient = webClientBuilder
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();
        this.circuitBreaker = registry.circuitBreaker("gemini-chat");
    }

    @Override
    public String streamChat(String systemPrompt,
                             List<Map<String, String>> conversationHistory,
                             String userMessage,
                             Consumer<String> tokenConsumer) {

        return circuitBreaker.executeSupplier(() -> {
            try {
                List<Map<String, Object>> contents = new ArrayList<>();

                // Add conversation history
                for (Map<String, String> msg : conversationHistory) {
                    contents.add(Map.of(
                        "role", msg.get("role").equals("user") ? "user" : "model",
                        "parts", List.of(Map.of("text", msg.get("content")))
                    ));
                }

                // Add current user message
                contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", userMessage))
                ));

                Map<String, Object> requestBody = Map.of(
                    "contents", contents,
                    "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                    "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 2048
                    )
                );

                log.debug("Calling Gemini API with model: {}", chatModel);

                Flux<Map> responseFlux = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:streamGenerateContent")
                        .queryParam("key", apiKey)
                        .build(chatModel))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(Map.class);

                StringBuilder fullResponse = new StringBuilder();

                responseFlux.toStream().forEach(chunk -> {
                    try {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> candidates =
                            (List<Map<String, Object>>) chunk.get("candidates");
                        if (candidates != null && !candidates.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> content =
                                (Map<String, Object>) candidates.get(0).get("content");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> parts =
                                (List<Map<String, Object>>) content.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                String text = (String) parts.get(0).get("text");
                                if (text != null) {
                                    fullResponse.append(text);
                                    tokenConsumer.accept(text);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing stream chunk", e);
                    }
                });

                log.debug("Gemini API call completed. Response length: {}", fullResponse.length());
                return fullResponse.toString();
            } catch (Exception e) {
                log.error("Error calling Gemini API", e);
                throw new ServiceUnavailableException("Gemini LLM", e.getMessage());
            }
        });
    }
}
