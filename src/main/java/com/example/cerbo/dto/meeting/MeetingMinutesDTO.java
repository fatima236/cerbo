package com.example.cerbo.dto.meeting;

import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.ProjectStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MeetingMinutesDTO {
    private Long meetingId;
    private String session; // Rempli manuellement
    private LocalDate meetingDate; // Généré automatiquement
    private List<AttendeeInfo> examiners;  // Liste des examinateurs (automatique)
    private List<AttendeeInfo> presentMembers; // Liste manuelle
    private List<ProjectReview> reviewedProjects; // Généré automatiquement + décisions manuelles
    private List<InvestigatorResponse> investigatorResponses; // Généré automatiquement + décisions manuelles

    @Data
    public static class AttendeeInfo {
        private Long userId;
        private String fullName;
        private String role;
        private boolean isManual;
    }

    @Data
    public static class ProjectReview {
        private String reference;
        private String title;
        private String principalInvestigator;
        private String decision; // Rempli manuellement
    }

    @Data
    public static class InvestigatorResponse {
        private String reference;
        private String title;
        private String principalInvestigator;
        private String decision; // Rempli manuellement
        private LocalDateTime responseDate;
    }

    @Data
    public class PresentMemberDTO {
        private Long userId;
        private String fullName;
        private String role;
    }

}