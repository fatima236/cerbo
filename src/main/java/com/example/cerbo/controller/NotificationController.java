package com.example.cerbo.controller;

import com.example.cerbo.dto.NotificationDTO;
import com.example.cerbo.entity.Notification;
import com.example.cerbo.entity.User;
import com.example.cerbo.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:3000")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;



    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(Authentication authentication) {
        String email = authentication.getName();
        List<Notification> notifications = notificationService.getNotificationsForUser(email);

        List<NotificationDTO> dtos = notificationService.getNotificationDTOsForUser(authentication.getName());
        return ResponseEntity.ok(dtos);

    }


    @GetMapping("/count-unread")
    public ResponseEntity<Integer> countUnread(Authentication authentication) {
        int count = notificationService.countUnreadNotifications(authentication.getName());

        return ResponseEntity.ok(count) ;
    }


    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        Notification notification = notificationService.markAsRead(id);

        return ResponseEntity.ok(notification);
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        String email = authentication.getName();
        notificationService.markAllAsRead(email);

        return ResponseEntity.ok().build();
    }


}