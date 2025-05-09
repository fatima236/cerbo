package com.example.cerbo.scheduler;

import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class NotificationScheduler {

    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;

    public NotificationScheduler(ProjectRepository projectRepository, NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 9 * * *") // tous les jours à 9h
    public void notifyEvaluatorsBeforeMeeting() {

    }
}
