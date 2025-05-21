package com.example.cerbo.controller;

import com.example.cerbo.dto.DocumentReviewDTO;
import com.example.cerbo.dto.RemarkDTO;
import com.example.cerbo.entity.DocumentReview;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentReviewRepository;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.RemarkRepository;
import com.example.cerbo.repository.UserRepository;
import com.example.cerbo.service.FileStorageService;
import com.example.cerbo.service.NotificationService;
import com.example.cerbo.service.RemarkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects/{projectId}/remarks")
@RequiredArgsConstructor
@Slf4j
public class RemarkController {

    private final RemarkRepository remarkRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final RemarkService remarkService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final DocumentReviewRepository documentReviewRepository;
    @GetMapping
    @PreAuthorize("@projectSecurity.isProjectMember(#projectId, authentication)")
    public ResponseEntity<List<RemarkDTO>> getProjectRemarks(@PathVariable Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found");
        }

        List<Remark> remarks = remarkRepository.findByProjectIdOrderByCreationDateDesc(projectId);
        return ResponseEntity.ok(remarks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('INVESTIGATEUR') and @projectSecurity.isProjectMember(#projectId, authentication)")
    public ResponseEntity<List<RemarkDTO>> getPendingRemarks(
            @PathVariable Long projectId,
            Authentication authentication) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User currentUser = userRepository.findByEmail(authentication.getName());
        if (currentUser == null) {
            throw new ResourceNotFoundException("User not found");
        }

        if (!project.getPrincipalInvestigator().equals(currentUser)) {
            throw new RuntimeException("Not authorized to view these remarks");
        }

        List<Remark> remarks = remarkRepository.findByProjectIdAndResponseIsNull(projectId);
        return ResponseEntity.ok(remarks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList()));
    }

    @PostMapping
    @PreAuthorize("hasRole('EVALUATEUR') and @projectSecurity.isProjectReviewer(#projectId, authentication)")
    public ResponseEntity<RemarkDTO> addRemark(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        Remark remark = remarkService.addRemark(projectId, content, authentication.getName());

        return ResponseEntity.ok(convertToDto(remark));
    }


    @GetMapping("/official-report")
    @PreAuthorize("hasRole('INVESTIGATEUR') and @projectSecurity.isProjectMember(#projectId, authentication)")
    public ResponseEntity<List<RemarkDTO>> getOfficialReportRemarks(
            @PathVariable Long projectId,
            Authentication authentication) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User currentUser = userRepository.findByEmail(authentication.getName());
        if (!project.getPrincipalInvestigator().equals(currentUser)) {
            throw new AccessDeniedException("Accès refusé");
        }

        // Récupérer seulement les remarques incluses dans le report officiel
        List<Remark> remarks = remarkRepository.findByProjectIdAndIncludedInReportTrue(projectId);

        return ResponseEntity.ok(remarks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList()));
    }

    @PostMapping(value = "/{remarkId}/response", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('INVESTIGATEUR') and @projectSecurity.isProjectMember(#projectId, authentication)")
    @Transactional // Ajout crucial
    public ResponseEntity<RemarkDTO> respondToRemark(
            @PathVariable Long projectId,
            @PathVariable Long remarkId,
            @RequestParam(value = "response", required = false) String responseText,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) throws IOException {

        // [1] Validation basique
        if ((responseText == null || responseText.trim().isEmpty()) && (file == null || file.isEmpty())) {
            throw new IllegalArgumentException("Réponse textuelle ou fichier requis");
        }

        // [2] Vérification projet
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

        // [3] Vérification délai
        if (project.getResponseDeadline() != null && LocalDateTime.now().isAfter(project.getResponseDeadline())) {
            project.setStatus(ProjectStatus.REJETE);
            projectRepository.save(project);
            throw new RuntimeException("Délai de réponse expiré");
        }

        // [4] Récupération remarque avec verrou
        Remark remark = remarkRepository.findByIdWithLock(remarkId)
                .orElseThrow(() -> new ResourceNotFoundException("Remarque non trouvée"));

        // [5] Vérification réponse existante
        if (remark.getResponse() != null) {
            throw new IllegalStateException("Réponse déjà existante");
        }

        // [6] Traitement fichier
        String filePath = null;
        if (file != null && !file.isEmpty()) {
            try {
                if (file.getSize() > 10_000_000) throw new IllegalArgumentException("Fichier trop volumineux");
                filePath = fileStorageService.storeFile(file);
            } catch (Exception e) {
                throw new RuntimeException("Échec du stockage du fichier", e);
            }
        }

        // [7] Mise à jour
        remark.setResponse(responseText != null ? responseText.trim() : null);
        remark.setResponseDate(LocalDateTime.now());
        remark.setResponseFilePath(filePath);

        Remark savedRemark = remarkRepository.save(remark);

        // [8] Notifications (version robuste)
        try {
            notifyStakeholders(project, savedRemark, authentication.getName());
        } catch (Exception e) {
            log.error("Échec de notification", e);
            // Ne pas bloquer malgré l'échec
        }

        return ResponseEntity.ok(convertToDto(savedRemark));
    }


    @GetMapping("/{remarkId}/response-file")
    @PreAuthorize("@projectSecurity.isProjectMember(#projectId, authentication)")
    public ResponseEntity<byte[]> downloadResponseFile(
            @PathVariable Long projectId,
            @PathVariable Long remarkId) throws IOException {

        Remark remark = remarkRepository.findById(remarkId)
                .orElseThrow(() -> new ResourceNotFoundException("Remark not found"));

        if (remark.getResponseFilePath() == null) {
            throw new ResourceNotFoundException("No response file available");
        }

        byte[] fileContent = fileStorageService.loadFileAsBytes(remark.getResponseFilePath());
        String contentType = determineContentType(remark.getResponseFilePath());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + getFileName(remark.getResponseFilePath()) + "\"")
                .body(fileContent);
    }

    private void notifyStakeholders(Project project, Remark remark, String responderEmail) {
        // Notifier l'admin (vous devrez adapter cette partie selon votre modèle)
        User admin = getAdminUser(); // Méthode à implémenter selon votre logique
        if (admin != null) {
            notificationService.createNotification(
                    admin.getEmail(),
                    "Nouvelle réponse reçue pour le projet: " + project.getTitle()
            );
        }

        // Notifier l'évaluateur
        if (remark.getReviewer() != null && !remark.getReviewer().getEmail().equals(responderEmail)) {
            notificationService.createNotification(
                    remark.getReviewer().getEmail(),
                    "Votre remarque a reçu une réponse - Projet: " + project.getTitle()
            );
        }
    }

    private User getAdminUser() {
        // Implémentez cette méthode pour récupérer un admin
        return userRepository.findByRolesContaining("ADMIN").stream()
                .findFirst()
                .orElse(null);
    }

    private String determineContentType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "pdf": return "application/pdf";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            default: return "application/octet-stream";
        }
    }

    private String getFileName(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }

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

        // Ajout des informations de réponse
        dto.setResponse(remark.getResponse());
        dto.setResponseDate(remark.getResponseDate());
        dto.setHasResponseFile(remark.getResponseFilePath() != null);

        return dto;
    }


    @GetMapping("/validated-remarks-grouped")
    public ResponseEntity<Map<String, Object>> getValidatedRemarksGrouped(
            @PathVariable Long projectId) {

        try {
            // 1. Récupérer les DocumentReview validés avec les documents associés
            List<DocumentReview> reviews = documentReviewRepository.findValidatedRemarksWithDocuments(projectId);

            // 2. Grouper par type de document
            Map<String, List<DocumentReviewDTO>> groupedByDocumentType = reviews.stream()
                    .map(this::convertToDTO)
                    .filter(dto -> dto.getDocumentType() != null) // Filtre les null
                    .collect(Collectors.groupingBy(
                            dto -> dto.getDocumentType().name(), // Utilisez le type de document
                            TreeMap::new, // Trie par ordre alphabétique
                            Collectors.toList()
                    ));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", groupedByDocumentType,
                    "count", reviews.size()
            ));

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des remarques", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Erreur serveur: " + e.getMessage()
            ));
        }
    }
    private DocumentReviewDTO convertToDTO(DocumentReview documentReview) {
        DocumentReviewDTO dto = new DocumentReviewDTO();
        dto.setId(documentReview.getId());
        dto.setContent(documentReview.getContent());
        dto.setCreationDate(documentReview.getCreationDate());
        dto.setStatus(documentReview.getStatus());
        dto.setResponse(documentReview.getResponse());

        // Informations du reviewer
        if (documentReview.getReviewer() != null) {
            dto.setReviewerId(documentReview.getReviewer().getId());
            dto.setReviewerNom(documentReview.getReviewer().getNom());
            dto.setReviewerPrenom(documentReview.getReviewer().getPrenom());
            dto.setReviewerEmail(documentReview.getReviewer().getEmail());
        }

        // Informations du document
        if (documentReview.getDocument() != null) {
            dto.setDocumentId(documentReview.getDocument().getId());
            dto.setDocumentName(documentReview.getDocument().getName());
            dto.setDocumentType(documentReview.getDocument().getType()); // Ajout crucial
        }

        return dto;
    }
}