package com.example.cerbo.controller;

import com.example.cerbo.dto.RemarkDTO;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.RemarkRepository;
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
    private final RemarkRepository remarkRepository;

    private RemarkDTO convertToDto(Remark remark) {
        RemarkDTO dto = new RemarkDTO();
        dto.setId(remark.getId());
        dto.setContent(remark.getContent());
        dto.setCreationDate(remark.getCreationDate());
        dto.setAdminStatus(remark.getAdminStatus() != null ? remark.getAdminStatus().toString() : null);
        dto.setValidationDate(remark.getValidationDate());

        if (remark.getReviewer() != null) {
            dto.setReviewerId(remark.getReviewer().getId());
            dto.setReviewerName(remark.getReviewer().getPrenom() + " " + remark.getReviewer().getNom());
        }

        dto.setResponse(remark.getResponse());
        dto.setResponseDate(remark.getResponseDate());
        dto.setHasResponseFile(remark.getResponseFilePath() != null);

        return dto;
    }

    @GetMapping("/preview")
    public ResponseEntity<List<RemarkDTO>> getRemarksForReport(@PathVariable Long projectId) {
        // Récupérer seulement les remarques validées par l'admin
        List<Remark> remarks = remarkRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.VALIDATED);
        return ResponseEntity.ok(remarks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList()));
    }


    @PostMapping("/send")
    public ResponseEntity<?> sendReportToInvestigator(@PathVariable Long projectId,
                                                      @RequestBody List<Long> remarkIds) {
        try {
            // 1. Générer le rapport (l'email est géré en interne avec try-catch)
            remarkReportService.generateAndSendReport(projectId, remarkIds);

            // 2. Récupérer le projet pour le délai
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

            // 1. Générer et envoyer le rapport
            remarkReportService.generateAndSendReport(projectId, remarkIds);

            // 2. Rafraîchir l'objet projet depuis la base
            project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé après mise à jour"));

            // 3. Vérifier que les remarques sont bien marquées
            List<Remark> includedRemarks = remarkRepository.findByProjectIdAndIncludedInReportTrue(projectId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Rapport généré avec succès. L'email a peut-être été envoyé.",
                    "deadline", project.getResponseDeadline().format(DateTimeFormatter.ISO_DATE_TIME),
                    "remarksIncluded", remarkIds.size(),
                    "remarkIds", includedRemarks.stream().map(Remark::getId).collect(Collectors.toList())

            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la génération du rapport: " + e.getMessage()
            ));
        }
    }
}