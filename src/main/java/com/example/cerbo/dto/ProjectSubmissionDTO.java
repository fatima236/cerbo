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
    @NotBlank(message = "La description des données est obligatoire")
    private String dataDescription;
    private String projectDescription;
    private String ethicalConsiderations;

    @NotNull(message = "L'investigateur principal est obligatoire")
    private Long principalInvestigatorId;

    private Set<Long> investigatorIds = new HashSet<>();

    // Fichiers obligatoires
    @NotNull(message = "La fiche info FR est obligatoire")
    private transient MultipartFile infoSheetFr;

    @NotNull(message = "La fiche info AR est obligatoire")
    private transient MultipartFile infoSheetAr;

    @NotNull(message = "Le formulaire de consentement FR est obligatoire")
    private transient MultipartFile consentFormFr;

    @NotNull(message = "Le formulaire de consentement AR est obligatoire")
    private transient MultipartFile consentFormAr;

    @NotNull(message = "L'attestation d'engagement est obligatoire")
    private transient MultipartFile commitmentCertificate;

    @NotNull(message = "Le CV est obligatoire")
    private transient MultipartFile cv;

    // Fichiers optionnels
    private transient MultipartFile projectDescriptionFile;
    private transient MultipartFile ethicalConsiderationsFile;
    private transient List<MultipartFile> otherDocuments;

    // Chemins des fichiers
    private String infoSheetFrPath;
    private String infoSheetArPath;
    private String consentFormFrPath;
    private String consentFormArPath;
    private String commitmentCertificatePath;
    private String cvPath;
    private String projectDescriptionFilePath;
    private String ethicalConsiderationsFilePath;
    private List<String> otherDocumentsPaths = new ArrayList<>();
    // Dans ProjectSubmissionDTO.java
    private transient MultipartFile motivationLetter;
    private String motivationLetterPath;

    // Ajoutez les getters/setters
    public MultipartFile getMotivationLetter() {
        return motivationLetter;
    }

    public void setMotivationLetter(MultipartFile motivationLetter) {
        this.motivationLetter = motivationLetter;
    }

    public String getMotivationLetterPath() {
        return motivationLetterPath;
    }

    public void setMotivationLetterPath(String motivationLetterPath) {
        this.motivationLetterPath = motivationLetterPath;
    }
}