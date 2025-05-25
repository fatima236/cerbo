package com.example.cerbo.scheduler;

import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Report;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.entity.enums.ReportStatus;
import com.example.cerbo.repository.ProjectRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@AllArgsConstructor
public class ReportDeadlineChecker {

    private final ProjectRepository projectRepository;

    @Scheduled(cron = "0 0 0 * * *")
    public void rejectProjectDeadline() {
        List<Project> projects = projectRepository.findAll();

        for (Project project : projects) {
            Report report = project.getLatestReport();
            if(report != null && report.getResponseDeadline() != null &&
                    !report.getResponsed()&&
                    report.getStatus() == ReportStatus.SENT&&
                    LocalDateTime.now().isAfter(report.getResponseDeadline())) {
                if(project.getStatus() != ProjectStatus.REJETE){
                    project.setStatus(ProjectStatus.REJETE);
                    projectRepository.save(project);
                }
            }
        }

    }

}
