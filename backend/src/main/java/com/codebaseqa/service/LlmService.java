package com.codebaseqa.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Interface for LLM (Large Language Model) services.
 * Allows swapping between different LLM providers (Gemini, OpenAI, Claude, etc.)
 * without changing the query pipeline.
 */
public interface LlmService {

    /**
     * Stream a chat response token-by-token.
     *
     * @param systemPrompt System instructions for the LLM
     * @param conversationHistory Previous messages in the conversation
     * @param userMessage The current user message (includes code context + question)
     * @param tokenConsumer Callback invoked for each token as it streams
     * @return The complete response text
     */
    String streamChat(String systemPrompt,
                      List<Map<String, String>> conversationHistory,
                      String userMessage,
                      Consumer<String> tokenConsumer);
}
