package com.example.cerbo.service;

import com.example.cerbo.dto.RemarkDTO;
import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RemarkReportService {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<Document> getValidatedDocuments(Long projectId) {
        return documentRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.VALIDATED);
    }

    @Transactional
    public void generateAndSendReport(Long projectId, List<Long> documentIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

        List<Document> documentsToInclude = documentRepository.findAllById(documentIds).stream()
                .filter(doc -> doc.getAdminStatus() == RemarkStatus.VALIDATED)
                .filter(doc -> doc.getProject().getId().equals(projectId))
                .filter(doc -> doc.getReviewRemark() != null && !doc.getReviewRemark().isEmpty())
                .collect(Collectors.toList());

        if (documentsToInclude.isEmpty()) {
            throw new ResourceNotFoundException("Aucune remarque valide sélectionnée");
        }

        documentsToInclude.forEach(doc -> {
            doc.setIncludedInReport(true);
            documentRepository.save(doc);
        });

        project.setResponseDeadline(LocalDateTime.now().plusDays(7));
        projectRepository.save(project);

        try {
            sendOfficialReport(project, documentsToInclude);
        } catch (Exception e) {
            System.err.println("Échec d'envoi d'email: " + e.getMessage());
        }
    }

    @Transactional
    public void generateAndSendReport(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

        List<Document> validatedDocuments = getValidatedDocuments(projectId);

        if (validatedDocuments.isEmpty()) {
            throw new ResourceNotFoundException("Aucune remarque validée pour ce projet");
        }

        validatedDocuments.forEach(doc -> {
            doc.setIncludedInReport(true);
            documentRepository.save(doc);
        });

        project.setResponseDeadline(LocalDateTime.now().plusDays(7));
        projectRepository.save(project);

        sendOfficialReport(project, validatedDocuments);
    }

    private void sendOfficialReport(Project project, List<Document> documents) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("project", project);
            templateData.put("remarks", documents.stream()
                    .map(this::convertToRemarkDTO)
                    .collect(Collectors.toList()));
            templateData.put("reportDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            templateData.put("deadline", project.getResponseDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            String recipientEmail = project.getPrincipalInvestigator().getEmail();
            String subject = "Rapport officiel des remarques - Projet: " + project.getTitle();

            emailService.sendEmail(
                    recipientEmail,
                    subject,
                    "remarks-official-report",
                    templateData
            );

            notificationService.createNotification(
                    recipientEmail,
                    "Rapport officiel reçu pour le projet: " + project.getTitle() +
                            ". Délai de réponse: " +
                            project.getResponseDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du rapport: " + e.getMessage());
            throw e;
        }
    }

    private RemarkDTO convertToRemarkDTO(Document document) {
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

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void checkExpiredProjects() {
        List<Project> projects = projectRepository.findByResponseDeadlineBeforeAndStatusNot(
                LocalDateTime.now(),
                ProjectStatus.REJETE
        );

        for (Project project : projects) {
            long pendingRemarks = documentRepository.countByProjectIdAndIncludedInReportTrueAndAdminResponseIsNull(project.getId());
            if (pendingRemarks > 0) {
                project.setStatus(ProjectStatus.REJETE);
                projectRepository.save(project);

                notificationService.createNotification(
                        project.getPrincipalInvestigator().getEmail(),
                        "Votre projet " + project.getTitle() + " a été rejeté car vous n'avez pas répondu " +
                                "aux remarques officielles avant la date limite (" +
                                project.getResponseDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ")"
                );
            }
        }
    }
}