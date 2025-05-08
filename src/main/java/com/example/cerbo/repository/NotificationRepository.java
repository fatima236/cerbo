package com.example.cerbo.repository;

import com.example.cerbo.entity.Notification;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderBySentDateDesc(User recipient);
    List<Notification> findByRecipientIdOrderBySentDateDesc(Long userId);
    int countByRecipientAndStatus(User recipient, NotificationStatus status);
}