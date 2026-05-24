package com.codebaseqa.dto.response;

import com.codebaseqa.model.Conversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {

    private UUID id;
    private UUID repoId;
    private String repoFullName;
    private String title;
    private List<MessageResponse> messages;
    private Instant createdAt;
    private Instant updatedAt;

    public static ConversationResponse from(Conversation conversation) {
        return ConversationResponse.builder()
            .id(conversation.getId())
            .repoId(conversation.getRepo().getId())
            .repoFullName(conversation.getRepo().getFullName())
            .title(conversation.getTitle())
            .messages(conversation.getMessages().stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList()))
            .createdAt(conversation.getCreatedAt())
            .updatedAt(conversation.getUpdatedAt())
            .build();
    }

    public static ConversationResponse fromWithoutMessages(Conversation conversation) {
        return ConversationResponse.builder()
            .id(conversation.getId())
            .repoId(conversation.getRepo().getId())
            .repoFullName(conversation.getRepo().getFullName())
            .title(conversation.getTitle())
            .messages(null)
            .createdAt(conversation.getCreatedAt())
            .updatedAt(conversation.getUpdatedAt())
            .build();
    }
}
