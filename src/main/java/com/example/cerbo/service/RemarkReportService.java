package com.example.cerbo.service;

import com.example.cerbo.dto.RemarkDTO;
import com.example.cerbo.entity.*;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.entity.enums.ReportStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.DocumentReviewRepository;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
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
    private final DocumentReviewRepository documentReviewRepository;
    private final ReportRepository reportRepository;
    private final ReportGenerationService reportGenerationService;

    @Transactional(readOnly = true)
    public List<Document> getValidatedDocuments(Long projectId) {
        return documentRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.VALIDATED);
    }


    @Transactional
    public Report generateAndSendReportx(Long projectId) throws Exception {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));





        project.setResponseDeadline(LocalDateTime.now().plusDays(7));
        project.setLastReportDate(LocalDateTime.now());
        project.setReportStatus(ReportStatus.ENVOYE);
        projectRepository.save(project);

        Report report = new Report();
        report.setProject(project);
        report.setCreationDate(LocalDateTime.now());
        reportRepository.save(report);

        Path pdfPath = reportGenerationService.generateReportPdf(report);

        report.setPath(pdfPath.toString());
        report.setFileName(pdfPath.getFileName().toString());
        reportRepository.save(report);


        return report;
    }

    @Transactional
    public void generateAndSendReport(Long projectId, List<Long> documentIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

        // Récupérer les DocumentReview valides pour ces documents

        List<DocumentReview> validReviews = documentReviewRepository.findByDocumentIdIn(documentIds).stream()
                .filter(review -> review.getAdminEmail() != null)
                .filter(review -> review.getAdminValidationDate() != null)
                .filter(review -> review.getRemark() != null && !review.getRemark().isEmpty())
                .collect(Collectors.toList());

        if (validReviews.isEmpty()) {
            throw new ResourceNotFoundException("Aucune remarque valide sélectionnée");
        }

        // Marquer les documents comme inclus dans le rapport
        validReviews.forEach(review -> {
            Document doc = review.getDocument();
            doc.setIncludedInReport(true);
            doc.setReportInclusionDate(LocalDateTime.now());
            documentRepository.save(doc);
        });

        project.setResponseDeadline(LocalDateTime.now().plusDays(7));
        project.setLastReportDate(LocalDateTime.now());
        project.setReportStatus(ReportStatus.ENVOYE);
        projectRepository.save(project);

        // Envoyer le rapport avec les DocumentReview valides
        sendOfficialReport(project, validReviews);
    }

    private void sendOfficialReport(Project project, List<DocumentReview> reviews) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("project", project);
        templateData.put("remarks", reviews.stream()
                .map(this::convertToRemarkDTO)
                .collect(Collectors.toList()));
        // ... reste du code inchangé
    }

    private RemarkDTO convertToRemarkDTO(DocumentReview review) {
        RemarkDTO dto = new RemarkDTO();
        dto.setId(review.getId());
        dto.setContent(review.getRemark());
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