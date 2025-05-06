package com.example.cerbo.service;

import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.RemarkRepository;
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

    private final RemarkRepository remarkRepository;
    private final ProjectRepository projectRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // Méthode existante conservée pour compatibilité
    @Transactional(readOnly = true)
    public List<Remark> getValidatedRemarks(Long projectId) {
        return remarkRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.VALIDATED);
    }

    // Nouvelle version avec sélection de remarques spécifiques
    @Transactional
    public void generateAndSendReport(Long projectId, List<Long> remarkIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

        // 1. Filtrer les remarques validées et sélectionnées
        List<Remark> remarksToInclude = remarkRepository.findAllById(remarkIds).stream()
                .filter(remark -> remark.getAdminStatus() == RemarkStatus.VALIDATED)
                .filter(remark -> remark.getProject().getId().equals(projectId))
                .collect(Collectors.toList());

        if (remarksToInclude.isEmpty()) {
            throw new ResourceNotFoundException("Aucune remarque valide sélectionnée");
        }

        // 2. Marquer les remarques comme incluses
        remarksToInclude.forEach(remark -> {
            remark.setIncludedInReport(true);
            remarkRepository.save(remark);
        });

        // 3. Définir le délai de réponse
        project.setResponseDeadline(LocalDateTime.now().plusDays(7));
        projectRepository.save(project);

        try {
            // 4. Essayer d'envoyer le rapport
            sendOfficialReport(project, remarksToInclude);
        } catch (Exception e) {
            // Logger l'erreur mais continuer l'exécution
            System.err.println("Échec d'envoi d'email: " + e.getMessage());
            // Vous pouvez aussi enregistrer cette erreur en base si nécessaire
        }
    }

    // Ancienne version conservée pour compatibilité (peut être supprimée si non utilisée)
    @Transactional
    public void generateAndSendReport(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

        List<Remark> validatedRemarks = getValidatedRemarks(projectId);

        if (validatedRemarks.isEmpty()) {
            throw new ResourceNotFoundException("Aucune remarque validée pour ce projet");
        }

        // Marquer toutes les remarques validées comme incluses
        validatedRemarks.forEach(remark -> {
            remark.setIncludedInReport(true);
            remarkRepository.save(remark);
        });

        project.setResponseDeadline(LocalDateTime.now().plusDays(7));
        projectRepository.save(project);

        sendOfficialReport(project, validatedRemarks);
    }

    // Méthode privée pour factoriser l'envoi du rapport
    private void sendOfficialReport(Project project, List<Remark> remarks) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("project", project);
            templateData.put("remarks", remarks);
            templateData.put("reportDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            templateData.put("deadline", project.getResponseDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            String recipientEmail = project.getPrincipalInvestigator().getEmail();
            String subject = "Rapport officiel des remarques - Projet: " + project.getTitle();

            // Envoyer l'email
            emailService.sendEmail(
                    recipientEmail,
                    subject,
                    "remarks-official-report",
                    templateData
            );

            // Créer une notification
            notificationService.createNotification(
                    recipientEmail,
                    "Rapport officiel reçu pour le projet: " + project.getTitle() +
                            ". Délai de réponse: " +
                            project.getResponseDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du rapport: " + e.getMessage());
            throw e; // Relancer l'exception pour une gestion plus haut niveau
        }
    }

    // Méthode pour vérifier les projets expirés (inchangée)
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void checkExpiredProjects() {
        List<Project> projects = projectRepository.findByResponseDeadlineBeforeAndStatusNot(
                LocalDateTime.now(),
                ProjectStatus.REJETE
        );

        for (Project project : projects) {
            long pendingRemarks = remarkRepository.countByProjectIdAndIncludedInReportTrueAndResponseIsNull(project.getId());
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