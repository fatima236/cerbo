package com.example.cerbo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSubmissionDTO {
    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 100, message = "Le titre ne doit pas dépasser 100 caractères")
    private String title;

    @NotBlank(message = "La durée de l'étude est obligatoire")
    private String studyDuration;

    @NotBlank(message = "La population cible est obligatoire")
    private String targetPopulation;

    @NotBlank(message = "Le type de consentement est obligatoire")
    private String consentType;

    private Boolean sampling = false;
    private String sampleType;
    private String sampleQuantity;
    private String fundingSource;
    private String fundingProgram;

    private String projectDescription;
    private String ethicalConsiderations;
    @Size(max = 50, message = "Status must be at most 50 characters")
    private String status;
    @NotNull(message = "L'investigateur principal est obligatoire")
    private Long principalInvestigatorId;

    private Set<Long> investigatorIds = new HashSet<>();

    // Fichiers (transients - non persistés)
    private transient MultipartFile infoSheetFr;
    private transient MultipartFile infoSheetAr;
    private transient MultipartFile consentFormFr;
    private transient MultipartFile consentFormAr;
    private transient MultipartFile commitmentCertificate;
    private transient MultipartFile cv;
    private transient MultipartFile projectDescriptionFile;
    private transient MultipartFile ethicalConsiderationsFile;
    private transient List<MultipartFile> otherDocuments;

    // Chemins des fichiers (persistés)
    private String infoSheetFrPath;
    private String infoSheetArPath;
    private String consentFormFrPath;
    private String consentFormArPath;
    private String commitmentCertificatePath;
    private String cvPath;
    private String projectDescriptionFilePath;
    private String ethicalConsiderationsFilePath;
    private List<String> otherDocumentsPaths = new ArrayList<>();
}