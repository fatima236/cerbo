package com.example.cerbo.service.dashboardService;

import com.example.cerbo.dto.DashboardStatsDto;


import com.example.cerbo.dto.DashboardStatsDto;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TrainingRepository trainingRepository;
    private final ArticleRepository articleRepository;

    @Override
    public DashboardStatsDto getStats() {
        long totalProjects = projectRepository.count();
        long pendingProjects = projectRepository.countByStatus(ProjectStatus.SOUMIS);
        long totalUsers = userRepository.count();
        long totalEvents = eventRepository.count();
        long totalTrainings = trainingRepository.count();
        long totalArticles = articleRepository.count();


        return new DashboardStatsDto(
                totalProjects,
                pendingProjects,
                totalEvents,
                totalUsers,
                totalArticles,
                totalTrainings
        );
    }
}
