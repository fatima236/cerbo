package com.example.cerbo.service;

import com.example.cerbo.entity.ApplicationEvent;
import com.example.cerbo.entity.Notification;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.EventType;
import com.example.cerbo.entity.enums.NotificationStatus;
import com.example.cerbo.repository.NotificationRepository;
import com.example.cerbo.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final ApplicationEventPublisher eventPublisher;
    private final EmailService emailService;
    private final JavaMailSender mailSender;
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private final UserRepository userRepository;

    /**
     * Récupère toutes les notifications d'un utilisateur
     */
    public List<Notification> getNotificationsForUser(String email) {
        User user = userRepository.findByEmail(email);
        return notificationRepository.findByRecipientOrderBySentDateDesc(user);
    }

    /**
     * Compte les notifications non lues d'un utilisateur
     */
    public int countUnreadNotifications(String email) {
        User user = userRepository.findByEmail(email);
        return notificationRepository.countByRecipientAndStatus(user, NotificationStatus.NON_LUE);
    }

    /**
     * Marque une notification comme lue
     */
    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));

        notification.setStatus(NotificationStatus.LUE);
        return notificationRepository.save(notification);
    }

    /**
     * Marque toutes les notifications d'un utilisateur comme lues
     */
    @Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email);
        List<Notification> notifications = notificationRepository.findByRecipientOrderBySentDateDesc(user);

        for (Notification notification : notifications) {
            notification.setStatus(NotificationStatus.LUE);
        }

        notificationRepository.saveAll(notifications);
    }

    /**
     * Crée une nouvelle notification pour un utilisateur
     */
    @Transactional
    public Notification createNotification(String recipientEmail, String content) {
        User recipient = userRepository.findByEmail(recipientEmail);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setContent(content);
        notification.setSentDate(LocalDateTime.now());
        notification.setStatus(NotificationStatus.NON_LUE);

        return notificationRepository.save(notification);
    }

    public void notifyProjectSubmitted(Project project) {
        Map<String, Object> data = new HashMap<>();
        data.put("projectId", project.getId());
        data.put("projectTitle", project.getTitle());

        eventPublisher.publishEvent(new ApplicationEvent(
                EventType.PROJECT_SUBMITTED, data));
    }

    @Async
    @EventListener
    public void handleProjectSubmitted(ApplicationEvent event) {
        if (event.getType() == EventType.PROJECT_SUBMITTED) {
            // 1. Notifier les admins
            List<User> admins = userRepository.findAll().stream()
                    .filter(user -> user.getRoles().contains("ADMIN"))
                    .collect(Collectors.toList());
            admins.forEach(admin -> {
                // Notification in-app
                createInAppNotification(admin, "Nouveau projet soumis",
                        "Le projet " + event.getData().get("projectTitle") + " a été soumis.");

                // Email
                emailService.sendEmail(
                        admin.getEmail(),
                        "Nouveau projet soumis",
                        "notification-project-submitted",
                        Map.of("projectName", event.getData().get("projectTitle"))
                );
            });
        }
    }

    private void createInAppNotification(User user, String title, String message) {
        Notification notification = new Notification();
        notification.setRecipient(user);
        notification.setContent(message);
        notification.setSentDate(LocalDateTime.now());
        notification.setStatus(NotificationStatus.NON_LUE);
        notificationRepository.save(notification);
    }

    @Async
    public void notifyProjectStatusChange(Project project, String comment) {
        String message = "Le statut de votre projet \"" + project.getTitle() + "\" a été modifié de " +
                project.getStatus().getDisplayName() + " à " +
                project.getStatus().getDisplayName() +
                (comment != null ? "\nCommentaire: " + comment : "");

        // Notification in-app
        Notification notification = new Notification();
        notification.setRecipient(project.getPrincipalInvestigator());
        notification.setContent(message);
        notification.setSentDate(LocalDateTime.now());
        notification.setStatus(NotificationStatus.NON_LUE);
        notificationRepository.save(notification);

        // Envoyer un email
        try {
            MimeMessage emailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(emailMessage, true);

            helper.setFrom("bouayadi.fatimazahra23@ump.ac.ma");
            helper.setTo(project.getPrincipalInvestigator().getEmail());
            helper.setSubject("Mise à jour du statut de votre projet " + project.getTitle());

            String htmlContent = "<html>" +
                    "<body style=\"font-family: Arial, sans-serif;\">" +
                    "<h2 style=\"color: #2e6c80;\">Statut du projet mis à jour</h2>" +
                    "<p>Le statut de votre projet <strong>" + project.getTitle() + "</strong> a été modifié :</p>" +
                    "<div style=\"background: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;\">" +
                    "<p><strong>Nouveau statut :</strong> " + project.getStatus().getDisplayName() + "</p>" +
                    (comment != null ? "<p><strong>Commentaire :</strong> " + comment + "</p>" : "") +
                    "</div>" +
                    "<p>Vous pouvez consulter votre projet en vous connectant à votre espace investigateur.</p>" +
                    "<a href=\"http://localhost:3000/investigateur/dashboard\" " +
                    "style=\"background-color: #4CAF50; color: white; padding: 10px 20px; " +
                    "text-decoration: none; border-radius: 5px; display: inline-block;\">" +
                    "Accéder à mes projets" +
                    "</a>" +
                    "</body>" +
                    "</html>";

            helper.setText(htmlContent, true);
            mailSender.send(emailMessage);

            log.info("Email de notification de changement de statut envoyé à {}",
                    project.getPrincipalInvestigator().getEmail());
        } catch (Exception e) {
            log.error("Échec d'envoi de l'email de notification de changement de statut", e);
        }
    }

    public void notifyAdmins(String message) {
        List<User> admins = userRepository.findByRolesContaining("ADMIN");
        admins.forEach(admin -> {
            log.info(                admin.getEmail(),
                    "Notification Admin",
                    "admin-notification",
                    Map.of("message", message));

        });
    }



}