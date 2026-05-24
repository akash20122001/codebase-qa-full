package com.codebaseqa.service.prompt;

import com.codebaseqa.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder pattern for constructing LLM prompts.
 * Assembles system instructions, retrieved code context, conversation history,
 * and the user question into a properly formatted prompt.
 */
public class PromptBuilder {

    private String systemPrompt = "";
    private final List<ChunkContext> codeChunks = new ArrayList<>();
    private final List<MessageContext> history = new ArrayList<>();
    private String question = "";

    public static PromptBuilder create() {
        return new PromptBuilder();
    }

    public PromptBuilder withSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return this;
    }

    public PromptBuilder withCodeChunk(String filePath, int startLine, int endLine,
                                        String chunkName, String content, String language) {
        this.codeChunks.add(new ChunkContext(filePath, startLine, endLine, chunkName, content, language));
        return this;
    }

    public PromptBuilder withCodeChunks(List<Object[]> rawChunks) {
        for (Object[] chunk : rawChunks) {
            this.codeChunks.add(new ChunkContext(
                (String) chunk[1],   // filePath
                (Integer) chunk[2],  // startLine
                (Integer) chunk[3],  // endLine
                (String) chunk[5],   // chunkName
                (String) chunk[6],   // content
                (String) chunk[7]    // language
            ));
        }
        return this;
    }

    public PromptBuilder withHistory(List<Message> messages) {
        for (Message msg : messages) {
            this.history.add(new MessageContext(msg.getRole().name(), msg.getContent()));
        }
        return this;
    }

    public PromptBuilder withQuestion(String question) {
        this.question = question;
        return this;
    }

    /**
     * Build the final user message that includes code context + question.
     * The system prompt is returned separately for the LLM API call.
     */
    public String buildUserMessage() {
        StringBuilder sb = new StringBuilder();

        // Code context section
        if (!codeChunks.isEmpty()) {
            sb.append("## Relevant Code Snippets\n\n");
            for (ChunkContext chunk : codeChunks) {
                sb.append("### ").append(chunk.filePath)
                  .append(" (lines ").append(chunk.startLine)
                  .append("-").append(chunk.endLine).append(")\n");
                if (chunk.chunkName != null && !chunk.chunkName.isEmpty()) {
                    sb.append("**").append(chunk.chunkName).append("**\n");
                }
                sb.append("```").append(chunk.language).append("\n");
                sb.append(chunk.content).append("\n");
                sb.append("```\n\n");
            }
        }

        // Question section
        sb.append("## Question\n\n");
        sb.append(question);

        return sb.toString();
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public List<MessageContext> getHistory() {
        return history;
    }

    public List<ChunkContext> getCodeChunks() {
        return codeChunks;
    }

    // Internal DTOs
    private record ChunkContext(String filePath, int startLine, int endLine,
                                 String chunkName, String content, String language) {}

    public record MessageContext(String role, String content) {}
}
