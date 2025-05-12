package com.example.cerbo.dto;

import com.example.cerbo.entity.enums.DocumentType;
import lombok.Data;

import java.util.Map;

@Data
public class RemarkStatisticsDTO {
    private long totalRemarks;
    private long pendingCount;
    private long validatedCount;
    private long rejectedCount;
    private Map<DocumentType, Long> countByDocumentType;
    private Map<String, Long> countByReviewer;
}
