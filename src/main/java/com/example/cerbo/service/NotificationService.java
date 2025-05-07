package com.example.cerbo.service;

import com.example.cerbo.entity.ApplicationEvent;
import com.example.cerbo.entity.Notification;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.EventType;
import com.example.cerbo.entity.enums.NotificationStatus;
import com.example.cerbo.repository.NotificationRepository;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
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
    public void notifyProjectStatusChange(Project project) {
        String message = "Votre projet " + project.getTitle() + " a été " +
                project.getStatus().getDisplayName();

        // Notification in-app
        Notification notification = new Notification();
        notification.setRecipient(project.getPrincipalInvestigator());
        notification.setContent(message);
        notification.setSentDate(LocalDateTime.now());
        notification.setStatus(NotificationStatus.NON_LUE);
        notificationRepository.save(notification);

        // Envoyer un email
        emailService.sendEmail(
                project.getPrincipalInvestigator().getEmail(),
                "Statut de votre projet mis à jour",
                "project-status-updated",
                Map.of(
                        "projectName", project.getTitle(),
                        "status", project.getStatus().getDisplayName()
                )
        );
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