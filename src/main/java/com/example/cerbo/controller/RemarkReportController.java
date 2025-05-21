package com.example.cerbo.controller;

import com.example.cerbo.dto.DocumentReviewDTO;

import java.nio.file.*;
import java.io.IOException;
import java.nio.file.Path;

import com.example.cerbo.entity.*;
import com.example.cerbo.entity.enums.DocumentType;
import com.example.cerbo.repository.*;
import com.example.cerbo.service.ProjectService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.example.cerbo.dto.RemarkDTO;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.entity.User;
import com.example.cerbo.service.RemarkReportService;
import com.example.cerbo.service.reportService.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.cerbo.repository.DocumentReviewRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/projects/{projectId}/report")
@RequiredArgsConstructor
public class RemarkReportController {

    private final RemarkReportService remarkReportService;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final DocumentReviewRepository documentReviewRepository;
    private final ReportService reportService;
    private final RemarkRepository remarkRepository;

    private DocumentReviewDTO convertToDTO(DocumentReview documentReview) {
        DocumentReviewDTO dto = new DocumentReviewDTO();
        dto.setId(documentReview.getId());
        dto.setReviewerNom(documentReview.getReviewer().getNom());
        dto.setStatus(documentReview.getStatus());
        dto.setContent(documentReview.getContent());
        dto.setCreationDate(documentReview.getCreationDate());
        dto.setResponse(documentReview.getResponse());
        if (documentReview.getReviewer() != null) {
            dto.setReviewerId(documentReview.getReviewer().getId());
            dto.setReviewerNom(documentReview.getReviewer().getNom());
            dto.setReviewerPrenom(documentReview.getReviewer().getPrenom());
            dto.setReviewerEmail(documentReview.getReviewer().getEmail());

        }

//        if (documentReview.getReport().getProject() != null) {
//            dto.setProjectId(documentReview.getReport().getProject().getId());
//            dto.setProjectTitle(documentReview.getReport().getProject().getTitle());
//        }

        dto.setDocumentName(documentReview.getReviewer().getFullName());



        return dto;
    }


//    private RemarkDTO convertToDto(Document document) {
//        RemarkDTO dto = new RemarkDTO();
//        dto.setId(document.getId());
//        dto.setContent(document.getReviewRemark());
//        dto.setCreationDate(document.getReviewDate());
//        dto.setAdminStatus(document.getAdminStatus() != null ? document.getAdminStatus().toString() : null);
//        dto.setValidationDate(document.getAdminValidationDate());
//
//        if (document.getReviewer() != null) {
//            dto.setReviewerId(document.getReviewer().getId());
//            dto.setReviewerName(document.getReviewer().getPrenom() + " " + document.getReviewer().getNom());
//        }
//
//        dto.setResponse(document.getAdminResponse());
//        dto.setResponseDate(document.getAdminResponseDate());
//        dto.setHasResponseFile(document.getResponseFilePath() != null);
//
//        return dto;
//    }

//    @GetMapping("/preview")
//    public ResponseEntity<List<RemarkDTO>> getRemarksForReport(@PathVariable Long projectId) {
//        List<Document> documents = documentRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.VALIDATED);
//        return ResponseEntity.ok(documents.stream()
//                .map(this::convertToDto)
//                .collect(Collectors.toList()));
//    }

    @PostMapping("/preview")
    public ResponseEntity<List<DocumentReviewDTO>> generateReportPreview(
            @PathVariable Long projectId,
            @RequestBody List<Long> documentIds) {

        List<DocumentReview > documentReviews = documentReviewRepository.findValidatedRemarksByProjectId(projectId);
        return ResponseEntity.ok(documentReviews.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendReportToInvestigator(@PathVariable Long projectId) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            Report report = reportService.finalizeAndSendReport(project.getLatestReport().getId());

            return ResponseEntity.ok(report);

        }
        catch (Exception e){
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la génération du rapport: " + e.getMessage()
            ));
        }
    }

//    @PostMapping("/send")
//    public ResponseEntity<?> sendReportToInvestigator(@PathVariable Long projectId,
//                                                      @RequestBody List<Long> documentReviewIds) {
//        try {
//            // Récupérer les DocumentReview qui répondent aux critères
//            List<DocumentReview> validReviews = documentReviewRepository.findAllById(documentReviewIds).stream()
//                    .filter(review -> review.getAdminEmail() != null)
//                    .filter(review -> review.getAdminValidationDate() != null)
//                    .filter(review -> review.getContent() != null && !review.getContent().isEmpty())
//                    .collect(Collectors.toList());
//
//            if (validReviews.isEmpty()) {
//                return ResponseEntity.badRequest().body(Map.of(
//                        "success", false,
//                        "message", "Aucune remarque valide sélectionnée (doit avoir admin_email, admin_validation_date et remark non vide)"
//                ));
//            }
//
//            // Extraire les IDs des documents associés
//            List<Long> validDocumentIds = validReviews.stream()
//                    .map(review -> review.getDocument().getId())
//                    .distinct()
//                    .collect(Collectors.toList());
//            Project project = projectRepository.findById(projectId)
//                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));
//
//            // Appeler le service avec les documents valides
//            Report report =remarkReportService.generateAndSendReportx(projectId);
//
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "message", "Rapport généré avec succès",
//                    "deadline", project.getResponseDeadline().format(DateTimeFormatter.ISO_DATE_TIME),
//                    "documentsIncluded", validDocumentIds.size(),
//                    "remarksIncluded", validReviews.size()
//            ));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "Erreur lors de la génération du rapport: " + e.getMessage()
//            ));
//        }
//    }

    @PostMapping("/genered")
    public ResponseEntity<Report> generedReport(@PathVariable Long projectId) {
        Project project =projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.badRequest().body(null);
        }
        List<Long> documentReview = documentReviewRepository.documentReviewValidated(projectId);
        Report report = reportService.createReport(projectId, documentReview);

        return ResponseEntity.ok(report);
    }
    @GetMapping(value = "/download", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("INVESTIGATEUR")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser) throws IOException {

        // 1. Vérification du projet et des permissions
        Project project = projectService.getProjectById(projectId);
        if (project == null) {
            System.err.println("❌ Projet non trouvé ID: " + projectId); // Log 2
            throw new RuntimeException("Projet non trouvé");
        }

        if (!project.getPrincipalInvestigator().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Accès refusé pour l'utilisateur ID: " + currentUser.getId());
        }

        // 2. Récupération du rapport
        Report report = remarkReportService.findLatestReportForProject(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun rapport disponible pour le projet ID: " + projectId));

        // 3. Construction du chemin du fichier
        Path filePath;
        try {
            // Solution temporaire - À supprimer après correction de la base de données
            String correctedPath = report.getFilePath().contains("reportstrapport")
                    ? report.getFilePath().replace("reportstrapport", "reports/rapport")
                    : report.getFilePath();

            filePath = Paths.get(correctedPath).normalize().toAbsolutePath();

            // Validation de sécurité: empêche les attaques par path traversal
            if (!filePath.startsWith(Paths.get("uploads").toAbsolutePath())) {
                throw new SecurityException("Chemin du rapport non autorisé");
            }
        } catch (InvalidPathException e) {
            throw new ResourceNotFoundException("Chemin du rapport invalide: " + report.getFilePath());
        }

        // 4. Vérification et lecture du fichier
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException(
                    String.format("Fichier rapport introuvable. Chemin: %s | Nom du fichier: %s",
                            filePath.getParent(), report.getFileName())
            );
        }

        // 5. Préparation de la réponse
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + report.getFileName() + "\"");
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(Files.size(filePath))
                .body(Files.readAllBytes(filePath));
    }

}