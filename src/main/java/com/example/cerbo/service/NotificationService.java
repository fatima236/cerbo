package com.example.cerbo.service;

import com.example.cerbo.entity.Notification;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.NotificationStatus;
import com.example.cerbo.repository.NotificationRepository;
import com.example.cerbo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

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
}