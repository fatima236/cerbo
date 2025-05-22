package com.example.cerbo.dto.meeting;

import com.example.cerbo.entity.MeetingAttendee;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO pour les participants aux réunions
 * Représente les invitations et participations aux réunions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class MeetingParticipantDTO {
    private Long id;                        // ID de l'invitation
    private Long userId;                    // ID de l'utilisateur invité
    private String userName;                // Nom complet de l'utilisateur
    private String userEmail;               // Email de l'utilisateur
    private Boolean addedManually;          // Invitation manuelle ou automatique
    private Long relatedProjectId;          // Projet lié (si invitation automatique)
    private String relatedProjectTitle;     // Titre du projet lié
    private String invitationType;          // "MANUAL" ou "AUTOMATIC"

    /**
     * Conversion depuis MeetingAttendee
     */
    public static MeetingParticipantDTO fromEntity(MeetingAttendee attendee) {
        if (attendee == null) {
            log.warn("Tentative de conversion d'un MeetingAttendee null");
            return null;
        }

        try {
            MeetingParticipantDTO dto = new MeetingParticipantDTO();
            dto.setId(attendee.getId());
            dto.setAddedManually(attendee.getAddedManually() != null ? attendee.getAddedManually() : false);
            dto.setInvitationType(dto.getAddedManually() ? "MANUAL" : "AUTOMATIC");

            // Informations de l'utilisateur
            if (attendee.getUser() != null) {
                dto.setUserId(attendee.getUser().getId());
                dto.setUserEmail(attendee.getUser().getEmail());
                dto.setUserName(buildUserFullName(attendee.getUser()));
            } else {
                log.warn("MeetingAttendee ID {} n'a pas d'utilisateur associé", attendee.getId());
                dto.setUserName("Utilisateur inconnu");
                dto.setUserEmail("inconnu@example.com");
            }

            // Informations du projet lié (si applicable)
            if (attendee.getRelatedProject() != null) {
                dto.setRelatedProjectId(attendee.getRelatedProject().getId());
                dto.setRelatedProjectTitle(attendee.getRelatedProject().getTitle());
            }

            return dto;
        } catch (Exception e) {
            log.error("Erreur lors de la conversion de MeetingAttendee ID {}: {}",
                    attendee.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Conversion de liste avec gestion d'erreurs
     */
    public static List<MeetingParticipantDTO> fromEntityList(List<MeetingAttendee> attendees) {
        if (attendees == null || attendees.isEmpty()) {
            return List.of();
        }

        return attendees.stream()
                .map(MeetingParticipantDTO::fromEntity)
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    /**
     * Méthode utilitaire réutilisable pour construire les noms complets
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

    /**
     * Méthode utilitaire pour déterminer si le participant est lié à un projet
     */
    public boolean hasRelatedProject() {
        return relatedProjectId != null;
    }

    /**
     * Méthode pour obtenir un résumé de l'invitation
     */
    public String getInvitationSummary() {
        String type = addedManually ? "Invitation manuelle" : "Invitation automatique";
        if (hasRelatedProject()) {
            return type + " (projet: " + relatedProjectTitle + ")";
        }
        return type;
    }
}