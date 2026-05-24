package com.codebaseqa.controller;

import com.codebaseqa.dto.response.ApiResponse;
import com.codebaseqa.dto.response.ConversationListPageResponse;
import com.codebaseqa.dto.response.ConversationResponse;
import com.codebaseqa.model.User;
import com.codebaseqa.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * List all conversations for the current user.
     * Optionally filter by repository.
     *
     * @param repoId Optional repository ID to filter by
     * @param page   Page number (default: 0)
     * @param size   Page size (default: 20, max: 50)
     * @param user   Authenticated user
     * @return Paginated list of conversations
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ConversationListPageResponse>> listConversations(
        @RequestParam(required = false) UUID repoId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal User user) {

        log.info("=== LIST CONVERSATIONS ENDPOINT HIT ===");
        log.info("User: {}", user != null ? user.getUsername() : "NULL");
        log.info("User ID: {}", user != null ? user.getId() : "NULL");
        log.info("RepoId filter: {}, page: {}, size: {}", repoId, page, size);

        ConversationListPageResponse response = conversationService.getUserConversations(user, repoId, page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get a specific conversation with all its messages.
     *
     * @param conversationId The conversation ID
     * @param user           Authenticated user
     * @return Conversation with messages
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
        @PathVariable UUID conversationId,
        @AuthenticationPrincipal User user) {

        log.info("=== GET CONVERSATION ENDPOINT HIT ===");
        log.info("User: {}", user != null ? user.getUsername() : "NULL");
        log.info("Conversation ID: {}", conversationId);

        ConversationResponse response = conversationService.getConversationById(conversationId, user);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete a conversation and all its messages.
     *
     * @param conversationId The conversation ID
     * @param user           Authenticated user
     * @return Success message
     */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteConversation(
        @PathVariable UUID conversationId,
        @AuthenticationPrincipal User user) {

        log.info("=== DELETE CONVERSATION ENDPOINT HIT ===");
        log.info("User: {}", user != null ? user.getUsername() : "NULL");
        log.info("Conversation ID: {}", conversationId);

        conversationService.deleteConversation(conversationId, user);

        return ResponseEntity.ok(ApiResponse.success(
            Map.of("message", "Conversation deleted successfully")
        ));
    }
}
