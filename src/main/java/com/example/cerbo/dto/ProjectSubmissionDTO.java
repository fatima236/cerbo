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

    // Pour les fichiers
    private MultipartFile projectDescriptionFile;
    private MultipartFile ethicalConsiderationsFile;
    private MultipartFile infoSheetFr;
    private MultipartFile infoSheetAr;
    private MultipartFile consentFormFr;
    private MultipartFile consentFormAr;
    private MultipartFile commitmentCertificate;
    private MultipartFile cv;
    private List<MultipartFile> otherDocuments;
}