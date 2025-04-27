package com.example.cerbo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Data
public class ProjectSubmissionDTO {
    private String title;
    private String studyDuration;
    private String targetPopulation;
    private String consentType;
    private Boolean sampling;
    private String sampleType;
    private String sampleQuantity;
    private String fundingSource;
    private String projectDescription;
    private String ethicalConsiderations;
    private Long principalInvestigatorId;
    private Set<Long> investigatorIds;
    private String infoSheetFr; // Changé de MultipartFile à String
    private String infoSheetAr;
    private String consentFormFr;
    private String consentFormAr;
    private String commitmentCertificate;
    private String cv;

    // Pour les fichiers
    private MultipartFile projectDescriptionFile;
    private MultipartFile ethicalConsiderationsFile;

    private List<MultipartFile> otherDocuments;
}