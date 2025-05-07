package com.example.cerbo.dto;

import com.example.cerbo.entity.Remark;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReportPreview {
    private Long projectId;
    private String projectTitle;
    private List<RemarkDTO> remarks;
    private int totalRemarks;
    private LocalDateTime estimatedDeadline;
    private String summary;

    public ReportPreview(Long projectId, String projectTitle, List<RemarkDTO> remarks) {
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.remarks = remarks;
        this.totalRemarks = remarks.size();
        this.estimatedDeadline = LocalDateTime.now().plusDays(7);
        this.summary = generateSummary();
    }

    private String generateSummary() {
        return String.format(
                "Rapport préliminaire pour le projet '%s' contenant %d remarques validées. " +
                        "Délai de réponse estimé: %s",
                projectTitle,
                totalRemarks,
                estimatedDeadline.toString()
        );
    }
}