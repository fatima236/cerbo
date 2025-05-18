package com.example.cerbo.controller;

import com.example.cerbo.dto.RemarkDTO;
import com.example.cerbo.entity.DocumentReview;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.User;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentReviewRepository;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.cerbo.dto.JwtTokenFilter.logger;

@RestController
@RequestMapping("/api/investigator/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INVESTIGATEUR')")
public class InvestigatorReportController {

    private final ProjectRepository projectRepository;
    private final DocumentReviewRepository documentReviewRepository;
    private final UserRepository userRepository;
    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getReportForProject(@PathVariable Long projectId,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        logger.debug("Début getReportForProject pour projectId: {}", projectId);

        try {
            // 1. Vérification authentification
            if (userDetails == null) {
                logger.warn("Tentative d'accès non authentifiée");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Non authentifié"));
            }

            logger.debug("UserDetails: {}", userDetails.getUsername());

            // 2. Récupération utilisateur
            User currentUser = userRepository.findByEmail(userDetails.getUsername());
            if (currentUser == null) {
                logger.error("Utilisateur non trouvé en base pour email: {}", userDetails.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Utilisateur non trouvé"));
            }

            logger.debug("CurrentUser ID: {}", currentUser.getId());

            // 3. Récupération projet
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> {
                        logger.error("Projet non trouvé ID: {}", projectId);
                        return new ResourceNotFoundException("Projet non trouvé");
                    });

            logger.debug("Projet trouvé - Investigateur principal ID: {}", project.getPrincipalInvestigator().getId());

            // 4. Vérification autorisation
            if (!project.getPrincipalInvestigator().getId().equals(currentUser.getId())) {
                logger.warn("Tentative d'accès non autorisée - User ID: {} - Investigateur projet ID: {}",
                        currentUser.getId(), project.getPrincipalInvestigator().getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Accès non autorisé"));
            }

            // 5. Récupération des reviews
            List<DocumentReview> reviews = documentReviewRepository.findByDocumentProjectId(projectId);
            logger.debug("Nombre de reviews trouvées: {}", reviews.size());

            List<DocumentReview> reportReviews = reviews.stream()
                    .filter(review -> {
                        boolean isValid = review.getAdminEmail() != null
                                && review.getAdminValidationDate() != null
                                && review.getContent() != null
                                && !review.getContent().isEmpty();
                        if (!isValid) {
                            logger.trace("Review filtrée ID: {}", review.getId());
                        }
                        return isValid;
                    })
                    .collect(Collectors.toList());

            logger.debug("Nombre de reviews valides: {}", reportReviews.size());

            if (reportReviews.isEmpty()) {
                logger.info("Aucun rapport valide pour le projet ID: {}", projectId);
                return ResponseEntity.ok(Map.of(
                        "message", "Aucun rapport disponible",
                        "hasReport", false
                ));
            }

            // 6. Construction réponse
            Map<String, Object> response = Map.of(
                    "project", Map.of(
                            "id", project.getId(),
                            "title", project.getTitle()
                    ),
                    "remarks", reportReviews.stream().map(this::convertToRemarkDTO).collect(Collectors.toList()),
                    "hasReport", true
            );

            logger.debug("Rapport généré avec succès");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("ERREUR CRITIQUE dans getReportForProject", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur technique",
                            "error", e.getMessage(),
                            "stacktrace", Arrays.stream(e.getStackTrace())
                                    .map(StackTraceElement::toString)
                                    .collect(Collectors.joining("\n"))
                    ));
        }
    }

    private RemarkDTO convertToRemarkDTO(DocumentReview review) {
        RemarkDTO dto = new RemarkDTO();
        dto.setId(review.getId());
        dto.setContent(review.getContent());
        dto.setCreationDate(review.getReviewDate());
        dto.setAdminStatus(review.getStatus() != null ? review.getStatus().toString() : null);
        dto.setValidationDate(review.getAdminValidationDate());
        dto.setAdminEmail(review.getAdminEmail());

        if (review.getReviewer() != null) {
            dto.setReviewerId(review.getReviewer().getId());
            dto.setReviewerName(review.getReviewer().getPrenom() + " " + review.getReviewer().getNom());
        }

        dto.setResponse(review.getAdminResponse());
        dto.setResponseDate(review.getAdminResponseDate());

        return dto;
    }

    @PostMapping("/respond/{reviewId}")
    public ResponseEntity<?> respondToRemark(@PathVariable Long reviewId,
                                             @RequestBody String response,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        // 1. Vérification de l'authentification
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Non authentifié"));
        }

        // 2. Conversion en User
        User currentUser = (User) userDetails;

        // 3. Récupération de la remarque
        DocumentReview review = documentReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Remarque non trouvée"));

        // 4. Vérification des autorisations
        if (!review.getDocument().getProject().getPrincipalInvestigator().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Non autorisé à répondre"));
        }

        // 5. Mise à jour de la réponse
        review.setAdminResponse(response);
        review.setAdminResponseDate(LocalDateTime.now());
        documentReviewRepository.save(review);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Réponse enregistrée"
        ));
    }


}