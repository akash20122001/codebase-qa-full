package com.codebaseqa.dto.response;

import com.codebaseqa.model.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private UUID id;
    private String role;
    private String content;
    private List<Message.Citation> citations;
    private Integer tokenCount;
    private Instant createdAt;

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
            .id(message.getId())
            .role(message.getRole().name())
            .content(message.getContent())
            .citations(message.getCitations())
            .tokenCount(message.getTokenCount())
            .createdAt(message.getCreatedAt())
            .build();
    }
}
