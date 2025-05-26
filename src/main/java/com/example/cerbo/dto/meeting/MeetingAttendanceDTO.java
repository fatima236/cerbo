package com.example.cerbo.dto.meeting;

import com.example.cerbo.entity.MeetingAttendance;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO pour les présences aux réunions
 * Représente la présence/absence des évaluateurs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class MeetingAttendanceDTO {
    private Long id;                    // ID de l'enregistrement de présence
    private Long evaluatorId;           // ID de l'évaluateur
    private String evaluatorName;       // Nom complet de l'évaluateur
    private String evaluatorEmail;      // Email de l'évaluateur
    private boolean present;            // Présent ou absent
    private String justification;       // Justification d'absence
    private boolean justified;          // Absence justifiée ou non
    private String attendanceStatus;    // Statut formaté pour l'affichage
    private boolean manual;     // true pour les membres ajoutés manuellement

    /**
     * Conversion depuis MeetingAttendance
     */
    public static MeetingAttendanceDTO fromEntity(MeetingAttendance attendance) {
        if (attendance == null) {
            log.warn("Tentative de conversion d'un MeetingAttendance null");
            return null;
        }

        try {
            MeetingAttendanceDTO dto = new MeetingAttendanceDTO();
            dto.setId(attendance.getId());
            dto.setPresent(attendance.isPresent());
            dto.setJustification(attendance.getJustification());
            dto.setJustified(attendance.isJustified());
            dto.setManual(attendance.isManual());

            // Informations de l'évaluateur
            if (attendance.getEvaluator() != null) {
                dto.setEvaluatorId(attendance.getEvaluator().getId());
                dto.setEvaluatorEmail(attendance.getEvaluator().getEmail());
                dto.setEvaluatorName(buildUserFullName(attendance.getEvaluator()));
            } else {
                log.warn("MeetingAttendance ID {} n'a pas d'évaluateur associé", attendance.getId());
                dto.setEvaluatorName("Évaluateur inconnu");
                dto.setEvaluatorEmail("inconnu@example.com");
            }

            // Calcul du statut formaté
            dto.setAttendanceStatus(calculateAttendanceStatus(dto.isPresent(), dto.isJustified()));

            return dto;
        } catch (Exception e) {
            log.error("Erreur lors de la conversion de MeetingAttendance ID {}: {}",
                    attendance.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Conversion de liste avec gestion d'erreurs
     */
    public static List<MeetingAttendanceDTO> fromEntityList(List<MeetingAttendance> attendances) {
        if (attendances == null || attendances.isEmpty()) {
            return List.of();
        }

        return attendances.stream()
                .map(MeetingAttendanceDTO::fromEntity)
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    /**
     * Méthode utilitaire pour construire les noms complets
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
     * Calcule le statut de présence formaté pour l'affichage
     */
    private static String calculateAttendanceStatus(boolean present, boolean justified) {
        if (present) {
            return "Présent";
        } else {
            return justified ? "Absent justifié" : "Absent non justifié";
        }
    }

    /**
     * Obtient la justification formatée pour l'affichage
     */
    public String getFormattedJustification() {
        if (present) {
            return "N/A - Présent";
        }

        if (justification == null || justification.trim().isEmpty()) {
            return "Aucune justification fournie";
        }

        return justification.trim();
    }
}