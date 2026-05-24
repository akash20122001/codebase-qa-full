package com.codebaseqa.service;

import com.codebaseqa.dto.request.AskQuestionRequest;
import com.codebaseqa.exception.RateLimitExceededException;
import com.codebaseqa.exception.RepoNotReadyException;
import com.codebaseqa.exception.ResourceNotFoundException;
import com.codebaseqa.exception.UnauthorizedException;
import com.codebaseqa.model.Conversation;
import com.codebaseqa.model.Message;
import com.codebaseqa.model.Repo;
import com.codebaseqa.model.User;
import com.codebaseqa.repository.ChunkRepository;
import com.codebaseqa.repository.ConversationRepository;
import com.codebaseqa.repository.MessageRepository;
import com.codebaseqa.repository.RepoRepository;
import com.codebaseqa.service.prompt.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryService {

    private final EmbeddingService embeddingService;
    private final LlmService llmService;
    private final ChunkRepository chunkRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final RepoRepository repoRepository;
    private final CacheService cacheService;
    private final RateLimitService rateLimitService;

    private static final int TOP_K_CHUNKS = 5;
    private static final int MAX_HISTORY_MESSAGES = 10;

    private static final String SYSTEM_PROMPT = """
        You are an expert code assistant helping developers understand their codebase.
        
        Your task is to answer questions about the code based on the provided code snippets.
        
        Guidelines:
        - Be concise and accurate
        - Reference specific files and line numbers when relevant
        - If the provided code doesn't contain enough information to answer, say so
        - Use markdown formatting for code blocks
        - Explain complex concepts clearly
        - If you see patterns or potential issues, mention them
        
        Always base your answers on the provided code context.
        """;

    /**
     * Stream an answer to a question using RAG (Retrieval-Augmented Generation).
     * Note: Not using @Transactional here because we need to commit the conversation
     * immediately so it's available for follow-up queries. Individual saves are
     * transactional by default.
     */
    public void streamAnswer(AskQuestionRequest request, User user, SseEmitter emitter) {
        try {
            // Step 1: Rate limiting
            if (rateLimitService.isRateLimited(user.getId())) {
                long retryAfter = rateLimitService.getResetTime(user.getId());
                throw new RateLimitExceededException(
                    "You have exceeded the rate limit. You can make 20 queries per hour.",
                    retryAfter
                );
            }

            // Step 2: Validate repo exists and is ready
            Repo repo = repoRepository.findById(request.getRepoId())
                .orElseThrow(() -> new ResourceNotFoundException("Repository", request.getRepoId().toString()));

            if (!repo.getUser().getId().equals(user.getId())) {
                throw new UnauthorizedException("repository", "query");
            }

            if (repo.getStatus() != Repo.RepoStatus.READY) {
                throw new RepoNotReadyException(repo.getStatus());
            }

            // Step 3: Check cache
            Object cachedResult = cacheService.getCachedQueryResult(request.getRepoId(), request.getQuestion());
            if (cachedResult instanceof String cachedAnswer) {
                log.debug("Cache hit for query on repo {}", request.getRepoId());
                sendCachedResponse(emitter, cachedAnswer);
                return;
            }

            // Step 4: Get or create conversation
            Conversation conversation;
            if (request.getConversationId() != null) {
                conversation = conversationRepository.findByIdAndUserId(request.getConversationId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation", request.getConversationId().toString()));
            } else {
                // Create new conversation with title from first question
                String title = request.getQuestion().length() > 50
                    ? request.getQuestion().substring(0, 47) + "..."
                    : request.getQuestion();

                conversation = Conversation.builder()
                    .user(user)
                    .repo(repo)
                    .title(title)
                    .messages(new ArrayList<>())
                    .build();
                conversation = conversationRepository.save(conversation);
            }

            // Step 5: Embed the question
            float[] queryEmbedding = embeddingService.embedText(request.getQuestion());
            String embeddingStr = embeddingService.toVectorString(queryEmbedding);

            // Step 6: Vector search for relevant code chunks
            List<Object[]> similarChunks = chunkRepository.findSimilarChunks(
                request.getRepoId(), embeddingStr, TOP_K_CHUNKS);

            // Step 7: Send citations first
            List<Message.Citation> citations = buildCitations(similarChunks);
            sendCitations(emitter, citations);

            // Step 8: Get conversation history
            List<Message> recentMessages = messageRepository
                .findTop10ByConversationIdOrderByCreatedAtDesc(conversation.getId());
            Collections.reverse(recentMessages); // Oldest first

            List<Map<String, String>> conversationHistory = recentMessages.stream()
                .map(msg -> Map.of(
                    "role", msg.getRole().name(),
                    "content", msg.getContent()
                ))
                .collect(Collectors.toList());

            // Step 9: Build prompt using Builder pattern
            String userMessage = PromptBuilder.create()
                .withSystemPrompt(SYSTEM_PROMPT)
                .withCodeChunks(similarChunks)
                .withQuestion(request.getQuestion())
                .buildUserMessage();

            // Step 10: Save user message
            Message userMsg = Message.builder()
                .conversation(conversation)
                .role(Message.MessageRole.user)
                .content(request.getQuestion())
                .citations(null)
                .tokenCount(0)
                .build();
            userMsg = messageRepository.save(userMsg);

            // Step 11: Stream LLM response
            StringBuilder fullAnswer = new StringBuilder();
            int[] tokenCount = {0};

            String answer = llmService.streamChat(
                SYSTEM_PROMPT,
                conversationHistory,
                userMessage,
                token -> {
                    fullAnswer.append(token);
                    tokenCount[0]++;
                    sendToken(emitter, token);
                }
            );

            // Step 12: Save assistant message
            Message assistantMsg = Message.builder()
                .conversation(conversation)
                .role(Message.MessageRole.assistant)
                .content(answer)
                .citations(citations)
                .tokenCount(tokenCount[0])
                .build();
            assistantMsg = messageRepository.save(assistantMsg);

            // Step 13: Cache the result
            cacheService.cacheQueryResult(request.getRepoId(), request.getQuestion(), answer);

            // Step 14: Send done event
            sendDone(emitter, assistantMsg.getId(), conversation.getId(), tokenCount[0]);

        } catch (Exception e) {
            log.error("Error streaming answer", e);
            sendError(emitter, "INTERNAL_ERROR", "An error occurred while processing your question: " + e.getMessage(), 0);
        }
    }

    private List<Message.Citation> buildCitations(List<Object[]> chunks) {
        List<Message.Citation> citations = new ArrayList<>();
        for (Object[] chunk : chunks) {
            Message.Citation citation = new Message.Citation();
            citation.setFilePath((String) chunk[1]);
            citation.setStartLine((Integer) chunk[2]);
            citation.setEndLine((Integer) chunk[3]);
            citation.setChunkName((String) chunk[5]);
            citation.setSnippet((String) chunk[6]);
            citations.add(citation);
        }
        return citations;
    }

    private void sendCitations(SseEmitter emitter, List<Message.Citation> citations) {
        try {
            emitter.send(SseEmitter.event()
                .name("citations")
                .data(citations));
        } catch (IOException e) {
            log.error("Error sending citations", e);
            emitter.completeWithError(e);
        }
    }

    private void sendToken(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event()
                .name("token")
                .data(Map.of("content", token)));
        } catch (IOException e) {
            log.error("Error sending token", e);
            emitter.completeWithError(e);
        }
    }

    private void sendDone(SseEmitter emitter, UUID messageId, UUID conversationId, int tokenCount) {
        try {
            emitter.send(SseEmitter.event()
                .name("done")
                .data(Map.of(
                    "messageId", messageId,
                    "conversationId", conversationId,
                    "tokenCount", tokenCount
                )));
            emitter.complete();
        } catch (IOException e) {
            log.error("Error sending done event", e);
            emitter.completeWithError(e);
        }
    }

    private void sendError(SseEmitter emitter, String code, String message, long retryAfter) {
        try {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("code", code);
            errorData.put("message", message);
            if (retryAfter > 0) {
                errorData.put("retryAfter", retryAfter);
            }

            emitter.send(SseEmitter.event()
                .name("error")
                .data(errorData));
            emitter.complete();
        } catch (IOException e) {
            log.error("Error sending error event", e);
            emitter.completeWithError(e);
        }
    }

    private void sendCachedResponse(SseEmitter emitter, String cachedAnswer) {
        try {
            // Send empty citations for cached responses
            emitter.send(SseEmitter.event()
                .name("citations")
                .data(Collections.emptyList()));

            // Send the full cached answer as a single token
            emitter.send(SseEmitter.event()
                .name("token")
                .data(Map.of("content", cachedAnswer)));

            // Send done event
            emitter.send(SseEmitter.event()
                .name("done")
                .data(Map.of(
                    "messageId", UUID.randomUUID(), // Placeholder for cached responses
                    "conversationId", UUID.randomUUID(),
                    "tokenCount", cachedAnswer.length() / 4 // Rough estimate
                )));

            emitter.complete();
        } catch (IOException e) {
            log.error("Error sending cached response", e);
            emitter.completeWithError(e);
        }
    }
}
