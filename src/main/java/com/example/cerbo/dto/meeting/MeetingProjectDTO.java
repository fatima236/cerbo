package com.example.cerbo.dto.meeting;

import com.example.cerbo.entity.MeetingProject;
import com.example.cerbo.entity.Project;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO spécialisé pour les projets dans l'ordre du jour
 * Contient seulement les informations nécessaires pour l'affichage de l'agenda
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class MeetingProjectDTO {
    private Long id;                    // ID du MeetingProject (pas du Project)
    private Long projectId;             // ID du projet réel
    private String projectTitle;        // Titre du projet
    private String projectReference;    // Référence du projet
    private String projectStatus;       // Statut du projet
    private Integer orderIndex;         // Position dans l'ordre du jour

    // Informations sur l'investigateur principal (utiles pour l'affichage)
    private String principalInvestigatorName;
    private String principalInvestigatorEmail;

    // Informations temporelles
    private LocalDateTime projectSubmissionDate;

    /**
     * Conversion depuis MeetingProject (avec accès au Project)
     */
    public static MeetingProjectDTO fromEntity(MeetingProject meetingProject) {
        if (meetingProject == null) {
            log.warn("Tentative de conversion d'un MeetingProject null");
            return null;
        }

        try {
            MeetingProjectDTO dto = new MeetingProjectDTO();
            dto.setId(meetingProject.getId());
            dto.setOrderIndex(meetingProject.getOrderIndex());

            // Informations du projet associé
            if (meetingProject.getProject() != null) {
                Project project = meetingProject.getProject();
                dto.setProjectId(project.getId());
                dto.setProjectTitle(project.getTitle());
                dto.setProjectReference(project.getReference());
                dto.setProjectStatus(project.getStatus() != null ? project.getStatus().toString() : "UNKNOWN");
                dto.setProjectSubmissionDate(project.getSubmissionDate());

                // Informations de l'investigateur principal
                if (project.getPrincipalInvestigator() != null) {
                    dto.setPrincipalInvestigatorName(
                            buildUserFullName(project.getPrincipalInvestigator())
                    );
                    dto.setPrincipalInvestigatorEmail(project.getPrincipalInvestigator().getEmail());
                } else {
                    dto.setPrincipalInvestigatorName("Investigateur non défini");
                    dto.setPrincipalInvestigatorEmail("non.defini@example.com");
                }
            } else {
                log.warn("MeetingProject ID {} n'a pas de projet associé", meetingProject.getId());
                dto.setProjectTitle("Projet non défini");
                dto.setProjectReference("REF-UNKNOWN");
                dto.setProjectStatus("UNKNOWN");
            }

            return dto;
        } catch (Exception e) {
            log.error("Erreur lors de la conversion de MeetingProject ID {}: {}",
                    meetingProject.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Conversion de liste avec filtrage des erreurs
     */
    public static List<MeetingProjectDTO> fromEntityList(List<MeetingProject> meetingProjects) {
        if (meetingProjects == null || meetingProjects.isEmpty()) {
            return List.of();
        }

        return meetingProjects.stream()
                .map(MeetingProjectDTO::fromEntity)
                .filter(dto -> dto != null) // Filtrer les conversions échouées
                .collect(Collectors.toList());
    }

    /**
     * Méthode utilitaire pour construire le nom complet d'un utilisateur
     */
    private static String buildUserFullName(com.example.cerbo.entity.User user) {
        if (user == null) return "Utilisateur inconnu";

        StringBuilder fullName = new StringBuilder();

        if (user.getCivilite() != null && !user.getCivilite().trim().isEmpty()) {
            fullName.append(user.getCivilite().trim()).append(" ");
        }
        if (user.getPrenom() != null && !user.getPrenom().trim().isEmpty()) {
            fullName.append(user.getPrenom().trim()).append(" ");
        }
        if (user.getNom() != null && !user.getNom().trim().isEmpty()) {
            fullName.append(user.getNom().trim());
        }

        String result = fullName.toString().trim();
        return result.isEmpty() ? "Nom non défini" : result;
    }
}