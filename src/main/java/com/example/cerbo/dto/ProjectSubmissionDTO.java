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

    // Fields for file paths (to be set after storage)
    private String infoSheetFrPath;
    private String infoSheetArPath;
    private String consentFormFrPath;
    private String consentFormArPath;
    private String commitmentCertificatePath;
    private String cvPath;
    private String projectDescriptionFilePath;
    private String ethicalConsiderationsFilePath;
    private List<String> otherDocumentsPaths;

    // Fields for receiving files (transient, not stored in DB)
    private transient MultipartFile infoSheetFr;
    private transient MultipartFile infoSheetAr;
    private transient MultipartFile consentFormFr;
    private transient MultipartFile consentFormAr;
    private transient MultipartFile commitmentCertificate;
    private transient MultipartFile cv;
    private transient MultipartFile projectDescriptionFile;
    private transient MultipartFile ethicalConsiderationsFile;
    private transient List<MultipartFile> otherDocuments;
}