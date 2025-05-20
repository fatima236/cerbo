package com.example.cerbo.controller;

import com.example.cerbo.dto.DocumentReviewDTO;
import com.example.cerbo.dto.DocumentReviewRequest;
import com.example.cerbo.entity.*;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.*;
import com.example.cerbo.repository.*;
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


    private DocumentReviewDTO convertToDTO(DocumentReview review) {
        DocumentReviewDTO dto = new DocumentReviewDTO();
        dto.setId(review.getId());
        dto.setStatus(review.getStatus());
        dto.setContent(review.getContent());
        dto.setCreationDate(review.getReviewDate());
        dto.setFinalized(review.isFinalized());
        dto.setResponse(review.getResponse());
        dto.setFinalSubmission(review.getFinal_submission());

        if (review.getReviewer() != null) {
            dto.setReviewerId(review.getReviewer().getId());
            dto.setReviewerNom(review.getReviewer().getNom());
            dto.setReviewerPrenom(review.getReviewer().getPrenom());
            dto.setReviewerEmail(review.getReviewer().getEmail());
        }

        if (review.getDocument() != null) {
            dto.setDocumentId(review.getDocument().getId());
            dto.setDocumentName(review.getDocument().getName());
            dto.setDocumentType(review.getDocument().getType());

            if (review.getDocument().getProject() != null) {
                dto.setProjectId(review.getDocument().getProject().getId());
                dto.setProjectTitle(review.getDocument().getProject().getTitle());
            }
        }

        return dto;
    }


    @PutMapping("/{documentId}/clear-review")
    @PreAuthorize("hasRole('EVALUATEUR')")
    public ResponseEntity<Void> clearReview(
            @PathVariable Long projectId,
            @PathVariable Long documentId,
            Authentication authentication) {

        User reviewer = Optional.ofNullable(userRepository.findByEmail(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        DocumentReview review = documentReviewRepository
                .findByDocumentIdAndReviewerId(documentId, reviewer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation non trouvée"));

        documentReviewRepository.delete(review);

        return ResponseEntity.noContent().build();
    }



    @PutMapping("/{documentId}/review")
    @PreAuthorize("hasRole('EVALUATEUR')")
    public ResponseEntity<DocumentReviewDTO> reviewDocument(
            @PathVariable Long projectId,
            @PathVariable Long documentId,
            @RequestBody DocumentReviewRequest request,
            Authentication authentication) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

        if (project.getReviewDeadline() != null && project.getReviewDeadline().isBefore(LocalDate.now())) {
            throw new BusinessException("La période d'évaluation est terminée");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé"));

        if (!document.getProject().getId().equals(projectId)) {
            throw new BusinessException("Ce document n'appartient pas au projet spécifié");
        }

        User reviewer = Optional.ofNullable(userRepository.findByEmail(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));


        DocumentReview existingReview = documentReviewRepository
                .findByDocumentIdAndReviewerId(documentId, reviewer.getId())
                .orElse(null);

        DocumentReview review;

        if (existingReview != null && existingReview.getFinal_submission()) {
            throw new BusinessException("La soumission finale a déjà été effectuée, vous ne pouvez plus modifier cette évaluation");
        }
        if (existingReview != null) {
            // Update existing review
            review = existingReview;
        } else {
            // Create new review
            review = new DocumentReview();
            review.setDocument(document);
            review.setReviewer(reviewer);
            review.setProject(project);
        }


        // Update review details
        review.setStatus(request.isValidated() ? RemarkStatus.VALIDATED : RemarkStatus.REVIEWED);
        review.setContent(request.getRemark());
        review.setReviewDate(LocalDateTime.now());
        review.setFinalized(false);

        DocumentReview savedReview = documentReviewRepository.save(review);

        return ResponseEntity.ok(convertToDTO(savedReview));
//        DocumentReview review = new DocumentReview();
//        review.setDocument(document);
//        review.setReviewer(reviewer);
//        review.setStatus(request.isValidated() ? RemarkStatus.VALIDATED : RemarkStatus.REVIEWED);
//        review.setContent(request.getRemark());
//        review.setReviewDate(LocalDateTime.now());
//        review.setProject(document.getProject());
//        review.setFinalized(false);
//
//        DocumentReview savedReview = documentReviewRepository.save(review);
//        return ResponseEntity.ok(convertToDTO(savedReview));
    }



    @GetMapping("/{documentId}/reviews")
    public ResponseEntity<List<DocumentReviewDTO>> getDocumentReviews(
            @PathVariable Long projectId,
            @PathVariable Long documentId) {

        List<DocumentReview> reviews = documentReviewRepository.findByDocumentId(documentId);
        return ResponseEntity.ok(reviews.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
    }
    @GetMapping("/{documentId}/reviews/me")
    @PreAuthorize("hasRole('EVALUATEUR')")
    public ResponseEntity<DocumentReviewDTO> getMyDocumentReview(
            @PathVariable Long projectId,
            @PathVariable Long documentId,
            Authentication authentication) {

        User reviewer = Optional.ofNullable(userRepository.findByEmail(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        DocumentReview review = documentReviewRepository
                .findByDocumentIdAndReviewerId(documentId, reviewer.getId())
                .orElse(null);

        if (review != null) {
            return ResponseEntity.ok(convertToDTO(review));
        } else {
            // Retourner un DTO vide avec juste les infos de base si aucune évaluation existe
            DocumentReviewDTO emptyReview = new DocumentReviewDTO();

            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé"));

            emptyReview.setDocumentId(document.getId());
            emptyReview.setDocumentName(document.getName());
            emptyReview.setDocumentType(document.getType());
            emptyReview.setStatus(RemarkStatus.PENDING);

            if (document.getProject() != null) {
                emptyReview.setProjectId(document.getProject().getId());
                emptyReview.setProjectTitle(document.getProject().getTitle());
            }

            return ResponseEntity.ok(emptyReview);
        }
    }

    @PostMapping("/submit-review")
    @PreAuthorize("hasRole('EVALUATEUR')")
    public ResponseEntity<Void> submitReview(
            @PathVariable Long projectId,
            Authentication authentication) {

        User reviewer = userRepository.findByEmail(authentication.getName());
        List<Document> documents = documentRepository.findByProjectId(projectId);

        long reviewedCount = documentReviewRepository.countByDocumentProjectIdAndReviewer(projectId, reviewer);
        if (reviewedCount < documents.size()) {
            throw new BusinessException("Vous devez évaluer tous les documents avant la soumission finale");
        }

        List<DocumentReview> evaluations = documentReviewRepository
                .findByDocumentProjectIdAndReviewer(projectId, reviewer);

        evaluations.forEach(eval -> {
            eval.setFinalized(true);
            eval.setFinal_submission(true);
            eval.setSubmissionDate(LocalDateTime.now());
        });

        documentReviewRepository.saveAll(evaluations);

        notificationService.sendNotification(
                userRepository.findByRolesContaining("ADMIN"),
                "Évaluation soumise",
                "Une évaluation a été soumise pour le projet \"" +
                        projectRepository.findById(projectId).get().getTitle() + "\".");

        return ResponseEntity.ok().build();
    }

    @GetMapping("/reviews/me")
    @PreAuthorize("hasRole('EVALUATEUR')")
    public ResponseEntity<List<DocumentReviewDTO>> getMyProjectReviews(
            @PathVariable Long projectId,
            Authentication authentication) {

        User reviewer = Optional.ofNullable(userRepository.findByEmail(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Récupérer tous les documents du projet
        List<Document> documents = documentRepository.findByProjectId(projectId);

        List<DocumentReviewDTO> result = documents.stream().map(document -> {
            DocumentReview review = documentReviewRepository
                    .findByDocumentIdAndReviewerId(document.getId(), reviewer.getId())
                    .orElse(null);

            if (review != null) {
                return convertToDTO(review);
            } else {
                DocumentReviewDTO emptyReview = new DocumentReviewDTO();
                emptyReview.setDocumentId(document.getId());
                emptyReview.setDocumentName(document.getName());
                emptyReview.setDocumentType(document.getType());
                emptyReview.setStatus(RemarkStatus.PENDING);
                emptyReview.setProjectId(projectId);
                emptyReview.setProjectTitle(document.getProject().getTitle());
                return emptyReview;
            }
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

}