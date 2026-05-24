package com.codebaseqa.service.impl;

import com.codebaseqa.dto.response.ConversationListPageResponse;
import com.codebaseqa.dto.response.ConversationListResponse;
import com.codebaseqa.dto.response.ConversationResponse;
import com.codebaseqa.exception.ResourceNotFoundException;
import com.codebaseqa.model.Conversation;
import com.codebaseqa.model.User;
import com.codebaseqa.repository.ConversationRepository;
import com.codebaseqa.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Override
    @Transactional(readOnly = true)
    public ConversationListPageResponse getUserConversations(User user, UUID repoId, int page, int size) {
        log.debug("Getting conversations for user {} (repoId: {}, page: {}, size: {})",
            user.getUsername(), repoId, page, size);

        // Validate and adjust page size
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            size = DEFAULT_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> conversationPage;

        // Filter by repo if specified
        if (repoId != null) {
            conversationPage = conversationRepository.findByUserIdAndRepoIdOrderByUpdatedAtDesc(
                user.getId(), repoId, pageable);
        } else {
            conversationPage = conversationRepository.findByUserIdOrderByUpdatedAtDesc(
                user.getId(), pageable);
        }

        // Map to response DTOs
        List<ConversationListResponse> conversations = conversationPage.getContent().stream()
            .map(conv -> ConversationListResponse.builder()
                .id(conv.getId())
                .repoId(conv.getRepo().getId())
                .repoFullName(conv.getRepo().getFullName())
                .title(conv.getTitle())
                .messageCount(conv.getMessages().size())
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .build())
            .collect(Collectors.toList());

        return ConversationListPageResponse.builder()
            .conversations(conversations)
            .totalCount(conversationPage.getTotalElements())
            .page(page)
            .size(size)
            .totalPages(conversationPage.getTotalPages())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversationById(UUID conversationId, User user) {
        log.debug("Getting conversation {} for user {}", conversationId, user.getUsername());

        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        return ConversationResponse.from(conversation);
    }

    @Override
    @Transactional
    public void deleteConversation(UUID conversationId, User user) {
        log.info("Deleting conversation {} for user {}", conversationId, user.getUsername());

        // Verify ownership before deleting
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        conversationRepository.delete(conversation);
        log.info("Conversation {} deleted successfully", conversationId);
    }
}
