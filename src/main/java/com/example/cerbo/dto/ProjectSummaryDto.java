package com.example.cerbo.dto;

import com.example.cerbo.entity.enums.ProjectStatus;

import java.time.LocalDateTime;

public class ProjectSummaryDto {
    private Long id;
    private String title;
    private ProjectStatus status;
    private LocalDateTime submissionDate;
    private String principalInvestigatorName;
    // getters/setters
}