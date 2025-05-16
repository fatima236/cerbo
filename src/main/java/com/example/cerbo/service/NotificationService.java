package com.example.cerbo.service;

import com.example.cerbo.dto.NotificationDTO;
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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final ApplicationEventPublisher eventPublisher;
    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    // Méthodes existantes inchangées
    public List<Notification> sendNotification(User recipient, String title, String content) {
        return sendNotification(Collections.singletonList(recipient), title, content);
    }

    public List<Notification> sendNotification(List<User> recipients, String title, String content) {
        List<Notification> notifications = recipients.stream()
                .map(user -> createNotification(user, title, content))
                .collect(Collectors.toList());

        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);

        savedNotifications.forEach(notification -> {
            messagingTemplate.convertAndSend(
                    "/topic/notifications/" + notification.getRecipient().getId(),
                    notification
            );
            sendEmailNotification(notification.getRecipient().getEmail(), title, content);
        });

        return savedNotifications;
    }

    public List<Notification> sendNotificationByIds(List<Long> recipients, String title, String content) {
        List<Notification> notifications = recipients.stream()
                .map(userId -> createNotification(userRepository.findById(userId).get(), title, content))
                .collect(Collectors.toList());

        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);

        savedNotifications.forEach(notification -> {
            messagingTemplate.convertAndSend(
                    "/topic/notifications/" + notification.getRecipient().getId(),
                    notification
            );
            sendEmailNotification(notification.getRecipient().getEmail(), title, content);
        });

        return savedNotifications;
    }

    // Ancienne version de createNotification (inchangée)
    @Transactional
    public Notification createNotification(String recipientEmail, String content) {
        User recipient = userRepository.findByEmail(recipientEmail);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setContent(content);
        notification.setSentDate(LocalDateTime.now());
        notification.setStatus(NotificationStatus.NON_LUE);

        // Ajout de l'envoi d'email
        sendEmailNotification(recipientEmail, "Nouvelle notification", content);

        return notificationRepository.save(notification);
    }

    // Nouvelle méthode pour l'envoi d'email
    @Async
    protected void sendEmailNotification(String recipientEmail, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText("Notification:\n\n" + content + "\n\nCordialement,\nVotre application");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email à {}", recipientEmail, e);
        }
    }

    // Méthode createNotification avec User (pour compatibilité)
    private Notification createNotification(User user, String title, String content) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRecipient(user);
        notification.setStatus(NotificationStatus.NON_LUE);
        notification.setSentDate(LocalDateTime.now());

        sendEmailNotification(user.getEmail(), title, content);

        return notification;
    }

    // Reste des méthodes existantes inchangées...
    public List<NotificationDTO> getNotificationDTOsForUser(String email) {
        return getNotificationsForUser(email).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private NotificationDTO convertToDTO(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getSentDate(),
                notification.getStatus()
        );
    }

    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByRecipientIdOrderBySentDateDesc(userId);
    }

    public List<Notification> getNotificationsForUser(String email) {
        User user = userRepository.findByEmail(email);
        return notificationRepository.findByRecipientOrderBySentDateDesc(user);
    }

    public int countUnreadNotifications(String email) {
        User user = userRepository.findByEmail(email);
        return notificationRepository.countByRecipientAndStatus(user, NotificationStatus.NON_LUE);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));

        notification.setStatus(NotificationStatus.LUE);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email);
        List<Notification> notifications = notificationRepository.findByRecipientOrderBySentDateDesc(user);

        notifications.forEach(notification -> notification.setStatus(NotificationStatus.LUE));
        notificationRepository.saveAll(notifications);
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
            List<User> admins = userRepository.findAll().stream()
                    .filter(user -> user.getRoles().contains("ADMIN"))
                    .collect(Collectors.toList());
            admins.forEach(admin -> {
                createNotification(admin, "Nouveau projet soumis",
                        "Le projet " + event.getData().get("projectTitle") + " a été soumis.");

                sendEmailNotification(
                        admin.getEmail(),
                        "Nouveau projet soumis",
                        "Le projet " + event.getData().get("projectTitle") + " a été soumis pour validation."
                );
            });
        }
    }

    @Async
    public void notifyProjectStatusChange(Project project) {
        String message = "Votre projet " + project.getTitle() + " a été " +
                project.getStatus().getDisplayName();

        Notification notification = createNotification(
                project.getPrincipalInvestigator(),
                "Statut de projet mis à jour",
                message
        );
        notificationRepository.save(notification);

        sendEmailNotification(
                project.getPrincipalInvestigator().getEmail(),
                "Statut de projet mis à jour",
                message
        );
    }

    public void notifyAdmins(String message) {
        List<User> admins = userRepository.findByRolesContaining("ADMIN");
        admins.forEach(admin -> {
            createNotification(admin, "Notification Admin", message);
            sendEmailNotification(admin.getEmail(), "Notification Admin", message);
        });
    }
}