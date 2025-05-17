package com.example.cerbo.dto;

import com.example.cerbo.entity.Report;
import com.example.cerbo.entity.enums.DocumentType;
import com.example.cerbo.entity.enums.ReportStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.example.cerbo.entity.enums.ProjectStatus;
import java.time.LocalDateTime;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class ProjectDTO {
    private Long id;
    private String reference;
    private LocalDateTime submissionDate;
    private LocalDateTime reviewDate;
    private LocalDateTime decisionDate;
    private ProjectStatus status;

    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    @NotBlank(message = "La durée de l'étude est obligatoire")
    private String studyDuration;

    @NotBlank(message = "La population cible est obligatoire")
    private String targetPopulation;

    @NotBlank(message = "Le type de consentement est obligatoire")
    private String consentType;

    private Boolean sampling;
    private String sampleType;
    private String sampleQuantity;
    private String fundingSource;
    private String fundingProgram;


    private Report report;

    @NotNull(message = "L'investigateur principal est obligatoire")
    private Long principalInvestigatorId;

    private List<Long> reviewerIds;
    private List<DocumentDTO> documents;


    // Documents
    private String projectDescription; // Texte descriptif
    private String ethicalConsiderations; // Texte des considérations
    private MultipartFile infoSheetFr;
    private MultipartFile infoSheetAr;
    private MultipartFile consentFormFr;
    private MultipartFile consentFormAr;
    private MultipartFile commitmentCertificate;
    private MultipartFile cvPrincipalInvestigator;
    private List<MultipartFile> otherDocuments;

    public void setPrincipalInvestigatorId(Long principalInvestigatorId) {
        this.principalInvestigatorId = principalInvestigatorId;
    }

    public void setReviewerIds(List<Long> reviewerIds) {
        this.reviewerIds = reviewerIds; }
}