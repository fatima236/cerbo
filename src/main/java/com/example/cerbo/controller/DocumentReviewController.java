package com.example.cerbo.controller;

import com.example.cerbo.dto.DocumentReviewDTO;
import com.example.cerbo.dto.DocumentReviewRequest;
import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.EvaluationStatus;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.BusinessException;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentRepository;
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

@RestController
@RequestMapping("/api/projects/{projectId}/documents")
@RequiredArgsConstructor
public class DocumentReviewController {

    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

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

        // Mise à jour du document
        document.setReviewStatus(request.isValidated() ? RemarkStatus.VALIDATED : RemarkStatus.REVIEWED);
        document.setReviewRemark(request.getRemark());
        document.setReviewer(reviewer);
        document.setReviewDate(LocalDateTime.now());

        Document savedDoc = documentRepository.save(document);

        return ResponseEntity.ok(convertToDTO(savedDoc));
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

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Vérifier que tous les documents sont traités
        List<Document> documents = documentRepository.findByProjectId(projectId);
        boolean allProcessed = documents.stream()
                .allMatch(d -> d.getReviewStatus() == RemarkStatus.VALIDATED
                        || d.getReviewStatus() == RemarkStatus.REVIEWED);

        if (!allProcessed) {
            throw new BusinessException("All documents must be reviewed or validated before submission");
        }



        // Marquer le projet comme évaluation soumise
        project.setEvaluationStatus(EvaluationStatus.SUBMITTED);
        project.setEvaluationSubmitDate(LocalDateTime.now());
        projectRepository.save(project);

        notificationService.sendNotification(userRepository.findByRolesContaining("ADMIN"),
                "Évaluation soumise",
                "Une évaluation a été soumise pour le projet \"" + project.getTitle() + "\".");

        return ResponseEntity.ok().build();
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
}