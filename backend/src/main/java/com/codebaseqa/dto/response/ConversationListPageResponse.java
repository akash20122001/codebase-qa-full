package com.codebaseqa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationListPageResponse {

    private List<ConversationListResponse> conversations;
    private Long totalCount;
    private Integer page;
    private Integer size;
    private Integer totalPages;
}
