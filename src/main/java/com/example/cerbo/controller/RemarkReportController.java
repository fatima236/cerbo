package com.example.cerbo.controller;

import com.example.cerbo.dto.RemarkDTO;
import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.service.RemarkReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/projects/{projectId}/report")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RemarkReportController {

    private final RemarkReportService remarkReportService;
    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;

    private RemarkDTO convertToDto(Document document) {
        RemarkDTO dto = new RemarkDTO();
        dto.setId(document.getId());
        dto.setContent(document.getReviewRemark());
        dto.setCreationDate(document.getReviewDate());
        dto.setAdminStatus(document.getAdminStatus() != null ? document.getAdminStatus().toString() : null);
        dto.setValidationDate(document.getAdminValidationDate());

        if (document.getReviewer() != null) {
            dto.setReviewerId(document.getReviewer().getId());
            dto.setReviewerName(document.getReviewer().getPrenom() + " " + document.getReviewer().getNom());
        }

        dto.setResponse(document.getAdminResponse());
        dto.setResponseDate(document.getAdminResponseDate());
        dto.setHasResponseFile(document.getResponseFilePath() != null);

        return dto;
    }

    @GetMapping("/preview")
    public ResponseEntity<List<RemarkDTO>> getRemarksForReport(@PathVariable Long projectId) {
        List<Document> documents = documentRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.VALIDATED);
        return ResponseEntity.ok(documents.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList()));
    }

    @PostMapping("/preview")
    public ResponseEntity<List<RemarkDTO>> generateReportPreview(
            @PathVariable Long projectId,
            @RequestBody List<Long> documentIds) {

        List<Document> documents = documentRepository.findAllById(documentIds);
        return ResponseEntity.ok(documents.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList()));
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendReportToInvestigator(@PathVariable Long projectId,
                                                      @RequestBody List<Long> documentIds) {
        try {
            // Vérifiez d'abord que les documents existent et sont validés
            List<Document> documents = documentRepository.findAllById(documentIds);
            long validatedCount = documents.stream()
                    .filter(d -> d.getAdminStatus() == RemarkStatus.VALIDATED)
                    .filter(d -> d.getReviewRemark() != null && !d.getReviewRemark().isEmpty())
                    .count();

            if (validatedCount != documentIds.size()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Certains documents ne sont pas validés ou n'ont pas de remarques"
                ));
            }

            remarkReportService.generateAndSendReport(projectId, documentIds);

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Rapport généré avec succès. L'email a peut-être été envoyé.",
                    "deadline", project.getResponseDeadline().format(DateTimeFormatter.ISO_DATE_TIME),
                    "documentsIncluded", documentIds.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la génération du rapport: " + e.getMessage()
            ));
        }
    }
}