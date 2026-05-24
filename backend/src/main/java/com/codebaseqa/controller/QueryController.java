package com.codebaseqa.controller;

import com.codebaseqa.dto.request.AskQuestionRequest;
import com.codebaseqa.model.User;
import com.codebaseqa.service.QueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@Slf4j
public class QueryController {

    private final QueryService queryService;

    /**
     * Ask a question about a repository.
     * Returns a Server-Sent Events (SSE) stream with:
     * - citations: Relevant code chunks
     * - token: Individual tokens as they stream from the LLM
     * - done: Final event with metadata
     * - error: If something goes wrong
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askQuestion(@Valid @RequestBody AskQuestionRequest request,
                                   @AuthenticationPrincipal User user) {
        log.info("User {} asking question on repo {}", user.getUsername(), request.getRepoId());

        // Create SSE emitter with 2-minute timeout
        SseEmitter emitter = new SseEmitter(120_000L);

        // Handle client disconnect
        emitter.onCompletion(() -> log.debug("SSE completed for user {}", user.getUsername()));
        emitter.onTimeout(() -> {
            log.warn("SSE timeout for user {}", user.getUsername());
            try {
                emitter.complete();
            } catch (Exception e) {
                // Ignore - emitter already completed
            }
        });
        emitter.onError(e -> {
            log.error("SSE error for user {}", user.getUsername(), e);
            try {
                emitter.completeWithError(e);
            } catch (Exception ex) {
                // Ignore - emitter already completed
            }
        });

        // Process async to not block the request thread
        CompletableFuture.runAsync(() -> {
            try {
                queryService.streamAnswer(request, user, emitter);
            } catch (Exception e) {
                log.error("Error processing query", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    // Ignore - emitter already completed
                    log.debug("Emitter already completed, ignoring error");
                }
            }
        });

        return emitter;
    }
}
