package com.example.cerbo.controller;

import com.example.cerbo.dto.DocumentReviewDTO;
import com.example.cerbo.dto.DocumentReviewRequest;
import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.DocumentReview;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.EvaluationStatus;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.BusinessException;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.DocumentReviewRepository;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.UserRepository;
import com.example.cerbo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects/{projectId}/documents")
@RequiredArgsConstructor
public class DocumentReviewController {

    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final DocumentReviewRepository documentReviewRepository;

    @PutMapping("/{documentId}/review")
    @PreAuthorize("hasRole('EVALUATEUR') and @projectSecurity.isProjectReviewer(#projectId, authentication)")
    public ResponseEntity<DocumentReviewDTO> reviewDocument(
            @PathVariable Long projectId,
            @PathVariable Long documentId,
            @RequestBody DocumentReviewRequest request,
            Authentication authentication) {

        // Validation
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Vérification de la date limite avec gestion du null
        if (project.getReviewDeadline() != null && project.getReviewDeadline().isBefore(LocalDate.now())) {
            throw new BusinessException("Evaluation period has ended");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        User reviewer = Optional.ofNullable(userRepository.findByEmail(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Créer une nouvelle évaluation au lieu de modifier le document
        DocumentReview review = new DocumentReview();
        review.setDocument(document);
        review.setReviewer(reviewer);
        review.setStatus(request.isValidated() ? RemarkStatus.VALIDATED : RemarkStatus.REVIEWED);
        review.setRemark(request.getRemark());
        review.setReviewDate(LocalDateTime.now());
        review.setFinalSubmission(false);

        documentReviewRepository.save(review);

        // Vous pourriez aussi mettre à jour le dernier statut du document si nécessaire
        document.setLatestReviewStatus(review.getStatus());
        documentRepository.save(document);

        return ResponseEntity.ok(convertToDTO(review));
    }

    @PutMapping("/set-deadline")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setReviewDeadline(
            @PathVariable Long projectId,
            @RequestParam LocalDate deadline) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        project.setReviewDeadline(deadline);
        projectRepository.save(project);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/submit-review")
    @PreAuthorize("hasRole('EVALUATEUR') and @projectSecurity.isProjectReviewer(#projectId, authentication)")
    public ResponseEntity<Void> submitReview(
            @PathVariable Long projectId,
            Authentication authentication) {

        // Récupérer le projet d'abord
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // 1. Vérifier que l'évaluateur a bien évalué tous les documents
        User reviewer = userRepository.findByEmail(authentication.getName());
        List<Document> documents = documentRepository.findByProjectId(projectId);

        long reviewedCount = documentReviewRepository.countByDocumentProjectIdAndReviewer(projectId, reviewer);
        if (reviewedCount < documents.size()) {
            throw new BusinessException("Vous devez évaluer tous les documents avant la soumission finale");
        }

        // 2. Marquer les évaluations comme "finalisées"
        List<DocumentReview> evaluations = documentReviewRepository
                .findByDocumentProjectIdAndReviewer(projectId, reviewer);

        evaluations.forEach(eval -> {
            eval.setFinalized(true);
            eval.setSubmissionDate(LocalDateTime.now());
        });

        documentReviewRepository.saveAll(evaluations);

        notificationService.sendNotification(userRepository.findByRolesContaining("ADMIN"),
                "Évaluation soumise",
                "Une évaluation a été soumise pour le projet \"" + project.getTitle() + "\".");

        return ResponseEntity.ok().build();
    }

    // Récupération des évaluations temporaires (pour affichage)
    @GetMapping("/my-reviews")
    public ResponseEntity<List<DocumentReviewDTO>> getMyReviews(
            @PathVariable Long projectId,
            Authentication authentication) {

        User reviewer = userRepository.findByEmail(authentication.getName());
        List<DocumentReview> reviews = documentReviewRepository
                .findByDocumentProjectIdAndReviewer(projectId, reviewer);

        return ResponseEntity.ok(reviews.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
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

    @GetMapping("/{documentId}/submission-status")
    public ResponseEntity<Boolean> getSubmissionStatus(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return ResponseEntity.ok(project.getEvaluationStatus() == EvaluationStatus.SUBMITTED);
    }

    private DocumentReviewDTO convertToDTO(DocumentReview review) {
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
}