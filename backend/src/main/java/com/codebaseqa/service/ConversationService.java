package com.codebaseqa.service;

import com.codebaseqa.dto.response.ConversationListPageResponse;
import com.codebaseqa.dto.response.ConversationResponse;
import com.codebaseqa.model.User;

import java.util.UUID;

/**
 * Service for managing conversations and their messages.
 * Handles conversation retrieval, deletion, and listing.
 */
public interface ConversationService {

    /**
     * Get all conversations for a user, optionally filtered by repository.
     *
     * @param user   The authenticated user
     * @param repoId Optional repository ID to filter by
     * @param page   Page number (0-indexed)
     * @param size   Page size (max 50)
     * @return Paginated list of conversations
     */
    ConversationListPageResponse getUserConversations(User user, UUID repoId, int page, int size);

    /**
     * Get a specific conversation with all its messages.
     *
     * @param conversationId The conversation ID
     * @param user           The authenticated user
     * @return Conversation with messages
     * @throws RuntimeException if conversation not found or user doesn't have access
     */
    ConversationResponse getConversationById(UUID conversationId, User user);

    /**
     * Delete a conversation and all its messages.
     *
     * @param conversationId The conversation ID
     * @param user           The authenticated user
     * @throws RuntimeException if conversation not found or user doesn't have access
     */
    void deleteConversation(UUID conversationId, User user);
}
