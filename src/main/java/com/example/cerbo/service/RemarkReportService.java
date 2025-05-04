package com.example.cerbo.service;

import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.RemarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RemarkReportService {

    private final RemarkRepository remarkRepository;
    private final ProjectRepository projectRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<Remark> getValidatedRemarks(Long projectId) {
        return remarkRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.VALIDATED);
    }

    @Transactional
    public void generateAndSendReport(Long projectId) {
        // Récupérer le projet
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

        // Récupérer les remarques validées
        List<Remark> validatedRemarks = getValidatedRemarks(projectId);

        if (validatedRemarks.isEmpty()) {
            throw new ResourceNotFoundException("Aucune remarque validée pour ce projet");
        }

        // Préparer les données pour le template
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("project", project);
        templateData.put("remarks", validatedRemarks);
        templateData.put("reportDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        // Envoyer l'email
        String recipientEmail = project.getPrincipalInvestigator().getEmail();
        String subject = "Rapport des remarques sur votre projet: " + project.getTitle();

        emailService.sendEmail(
                recipientEmail,
                subject,
                "remarks-report",
                templateData
        );
    }
}