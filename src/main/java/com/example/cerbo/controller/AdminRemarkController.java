package com.example.cerbo.controller;

import com.example.cerbo.dto.DocumentReviewDTO;
import com.example.cerbo.dto.OrganizedRemarksDTO;
import com.example.cerbo.dto.RemarkResponseDTO;
import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.DocumentReview;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.DocumentReviewRepository;
import com.example.cerbo.repository.RemarkRepository;
import com.example.cerbo.service.AdminRemarkService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.cerbo.dto.ReportPreview;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/remarks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRemarkController {

    private final AdminRemarkService adminRemarkService;
    private final DocumentRepository documentRepository;
    private final DocumentReviewRepository documentReviewRepository;


    @GetMapping("/pending")
    public ResponseEntity<List<RemarkResponseDTO>> getPendingRemarks() {
        List<Document> documents = documentRepository.findByReviewStatusAndReviewRemarkIsNotNull(RemarkStatus.REVIEWED);

        return ResponseEntity.ok(documents.stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<List<RemarkResponseDTO>> getProjectRemarks(@PathVariable Long projectId) {
        List<Document> documents = documentRepository.findByProjectIdAndReviewRemarkIsNotNull(projectId);
        List<DocumentReviewDTO> reviews = documents.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(documents.stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    @GetMapping("/projects/{projectId}/evaluations")
    public ResponseEntity<List<DocumentReviewDTO>> getAllEvaluationsForProject(
            @PathVariable Long projectId) {

        // Récupérer tous les documents du projet
        List<Document> documents = documentRepository.findByProjectId(projectId);

        // Récupérer toutes les évaluations pour ces documents
        List<DocumentReviewDTO> allEvaluations = documents.stream()
                .flatMap(doc -> documentReviewRepository.findByDocumentId(doc.getId()).stream())
                .map(this::convertReviewToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(allEvaluations);
    }

    @GetMapping("/projects/{projectId}/final-evaluations")
    public ResponseEntity<List<DocumentReviewDTO>> getFinalEvaluationsForProject(
            @PathVariable Long projectId) {

        List<DocumentReview> evaluations = documentReviewRepository
                .findByDocumentProjectIdAndFinalizedTrue(projectId);

        return ResponseEntity.ok(evaluations.stream()
                .map(this::convertReviewToDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/projects/{projectId}/evaluations-by-document")
    public ResponseEntity<Map<String, List<DocumentReviewDTO>>> getEvaluationsGroupedByDocument(
            @PathVariable Long projectId) {

        List<DocumentReview> evaluations = documentReviewRepository.findByDocumentProjectId(projectId);

        Map<String, List<DocumentReviewDTO>> groupedEvaluations = evaluations.stream()
                .map(this::convertReviewToDTO)
                .collect(Collectors.groupingBy(DocumentReviewDTO::getDocumentName));

        return ResponseEntity.ok(groupedEvaluations);
    }

    private DocumentReviewDTO convertReviewToDTO(DocumentReview review) {
        DocumentReviewDTO dto = new DocumentReviewDTO();
        dto.setId(review.getId());
        dto.setReviewStatus(review.getStatus());
        dto.setReviewRemark(review.getRemark());
        dto.setReviewDate(review.getReviewDate());

        if (review.getReviewer() != null) {
            dto.setReviewerId(review.getReviewer().getId());
            dto.setReviewerNom(review.getReviewer().getNom());
            dto.setReviewerPrenom(review.getReviewer().getPrenom());
            dto.setReviewerEmail(review.getReviewer().getEmail());
        }

        if (review.getDocument() != null) {
            dto.setDocumentId(review.getDocument().getId());
            dto.setDocumentName(review.getDocument().getName());
            dto.setDocumentType(review.getDocument().getType().name());

            if (review.getDocument().getProject() != null) {
                dto.setProjectId(review.getDocument().getProject().getId());
                dto.setProjectTitle(review.getDocument().getProject().getTitle());
            }
        }

        return dto;
    }

    @PutMapping("/{documentReviewId}/content-and-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRemarkContentAndStatus(
            @PathVariable Long documentReviewId,
            @RequestBody UpdateRemarkRequest request,
            Authentication authentication) {

        try {
            DocumentReview review = documentReviewRepository.findById(documentReviewId)
                    .orElseThrow(() -> new ResourceNotFoundException("Évaluation non trouvée"));

            // Vérification si déjà validé/rejeté et tentative de modification
            if ((review.getStatus() == RemarkStatus.VALIDATED || review.getStatus() == RemarkStatus.REJECTED)
                    && !request.getStatus().equals(review.getStatus().name())) {
                return ResponseEntity.badRequest().body("Une remarque validée ou rejetée ne peut plus changer de statut");
            }

            // Mise à jour du contenu si autorisé
            if (review.getStatus() != RemarkStatus.VALIDATED && review.getStatus() != RemarkStatus.REJECTED) {
                review.setRemark(request.getContent());
                review.setAdminComment(request.getComment());
            }

            // Mise à jour du statut
            if (request.getStatus() != null) {
                RemarkStatus newStatus = RemarkStatus.valueOf(request.getStatus());
                review.setStatus(newStatus);

                if (newStatus == RemarkStatus.VALIDATED || newStatus == RemarkStatus.REJECTED) {
                    review.setAdminResponse(request.getAdminResponse());
                    review.setAdminResponseDate(LocalDateTime.now());
                    review.setAdminEmail(authentication.getName());
                }
            }

            DocumentReview updatedReview = documentReviewRepository.save(review);
            return ResponseEntity.ok(convertReviewToDTO(updatedReview));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Statut invalide. Valeurs acceptées: PENDING, VALIDATED, REJECTED");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur serveur: " + e.getMessage());
        }
    }

    @GetMapping("/projects/{projectId}/validated")
    public ResponseEntity<List<RemarkResponseDTO>> getValidatedRemarks(@PathVariable Long projectId) {
        List<Document> documents = documentRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.VALIDATED);
        return ResponseEntity.ok(documents.stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    @PostMapping("/projects/{projectId}/generate-report")
    public ResponseEntity<ReportPreview> generateReportPreview(
            @PathVariable Long projectId,
            @RequestBody List<Long> documentIds) {

        ReportPreview preview = adminRemarkService.generateReportPreview(projectId, documentIds);
        return ResponseEntity.ok(preview);
    }

    @PutMapping("/{documentReviewId}/content")
    public ResponseEntity<?> updateRemarkContent(
            @PathVariable Long documentReviewId,
            @RequestBody UpdateRemarkRequest request,
            Authentication authentication) {

        try {
            DocumentReview review = documentReviewRepository.findById(documentReviewId)
                    .orElseThrow(() -> new ResourceNotFoundException("Évaluation non trouvée"));

            if (!review.isFinalized()) {
                return ResponseEntity.badRequest().body("Seules les évaluations finalisées peuvent être modifiées");
            }

            // Empêcher la modification si déjà validé
            if (review.getStatus() == RemarkStatus.VALIDATED) {
                return ResponseEntity.badRequest().body("Une remarque validée ne peut plus être modifiée");
            }

            review.setRemark(request.getContent());
            review.setAdminComment(request.getComment());
            review.setAdminResponse(request.getAdminResponse());
            review.setAdminResponseDate(LocalDateTime.now());
            review.setAdminEmail(authentication.getName());

            DocumentReview updatedReview = documentReviewRepository.save(review);
            return ResponseEntity.ok(convertReviewToDTO(updatedReview));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur serveur");
        }
    }

    @GetMapping("/projects/{projectId}/organized")
    public ResponseEntity<OrganizedRemarksDTO> getOrganizedRemarks(@PathVariable Long projectId) {
        OrganizedRemarksDTO organizedRemarks = adminRemarkService.getOrganizedRemarks(projectId);
        return ResponseEntity.ok(organizedRemarks);
    }

    private RemarkResponseDTO convertToDto(Document document) {
        RemarkResponseDTO dto = new RemarkResponseDTO();
        dto.setId(document.getId());
        dto.setContent(document.getReviewRemark());
        dto.setCreationDate(document.getReviewDate());
        dto.setAdminStatus(document.getAdminStatus() != null ? document.getAdminStatus().name() : null);
        dto.setValidationDate(document.getAdminValidationDate());
        dto.setComment(document.getAdminComment());
        dto.setAdminResponse(document.getAdminResponse());
        dto.setAdminResponseDate(document.getAdminResponseDate());

        if (document.getReviewer() != null) {
            RemarkResponseDTO.ReviewerDTO reviewerDto = new RemarkResponseDTO.ReviewerDTO();
            reviewerDto.setEmail(document.getReviewer().getEmail());
            reviewerDto.setPrenom(document.getReviewer().getPrenom());
            reviewerDto.setNom(document.getReviewer().getNom());
            dto.setReviewer(reviewerDto);
        }

        return dto;
    }

    private DocumentReviewDTO convertToDTO(Document document) {
        DocumentReviewDTO dto = new DocumentReviewDTO();
        dto.setId(document.getId());
        dto.setName(document.getName());
        dto.setReviewStatus(document.getReviewStatus());
        dto.setReviewRemark(document.getReviewRemark());
        dto.setReviewDate(document.getReviewDate());
        if (document.getReviewer() != null) {
            dto.setReviewerId(document.getReviewer().getId());
            dto.setReviewerNom(document.getReviewer().getNom());
            dto.setReviewerPrenom(document.getReviewer().getPrenom());
            dto.setReviewerEmail(document.getReviewer().getEmail());

        }

        if (document.getProject() != null) {
            dto.setProjectId(document.getProject().getId());
            dto.setProjectTitle(document.getProject().getTitle());
        }

        dto.setDocumentName(document.getName());
        dto.setDocumentType(document.getType().name());



        return dto;
    }
}

@Data
class UpdateRemarkRequest {
    private String content;
    private String comment;
    private String adminResponse;
    private String status;
}