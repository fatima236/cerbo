package com.example.cerbo.dto;

import com.example.cerbo.entity.enums.DocumentType;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class OrganizedRemarksDTO {
    private Map<DocumentType, List<RemarkResponseDTO>> byDocumentType;
    private Map<String, List<RemarkResponseDTO>> byReviewer;
    private Map<String, List<RemarkResponseDTO>> byStatus;
    private RemarkStatisticsDTO statistics;
}

