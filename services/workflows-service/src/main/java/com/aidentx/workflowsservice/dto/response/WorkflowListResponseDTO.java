package com.aidentx.workflowsservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Paginated list response for workflows
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowListResponseDTO {

    private List<WorkflowResponseDTO> content;
    private PageInfo pageInfo;

    /**
     * Factory method from Spring Data Page
     */
    public static WorkflowListResponseDTO from(Page<com.aidentx.workflowsservice.entity.Workflow> page) {
        return WorkflowListResponseDTO.builder()
                .content(page.getContent().stream()
                        .map(WorkflowResponseDTO::from)
                        .collect(Collectors.toList()))
                .pageInfo(PageInfo.builder()
                        .page(page.getNumber())
                        .size(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
