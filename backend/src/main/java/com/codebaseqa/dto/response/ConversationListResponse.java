package com.codebaseqa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationListResponse {

    private UUID id;
    private UUID repoId;
    private String repoFullName;
    private String title;
    private Integer messageCount;
    private Instant createdAt;
    private Instant updatedAt;
}
