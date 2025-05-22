package com.example.cerbo.dto.meeting;

import com.example.cerbo.entity.Meeting;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO principal pour les réunions
 * Offre différentes représentations selon le contexte d'utilisation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class MeetingDTO {
    private Long id;
    private String month;
    private String status;
    private int year;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime time;

    // Relations optionnelles (selon le contexte)
    private List<MeetingProjectDTO> agendaItems;
    private List<MeetingParticipantDTO> participants;
    private List<MeetingAttendanceDTO> attendances;

    // Compteurs pour les vues simplifiées
    private int agendaItemsCount;
    private int participantsCount;
    private int attendancesCount;

    /**
     * Version complète pour les pages de détail
     * Inclut toutes les relations avec leurs données complètes
     */
    public static MeetingDTO createComplete(Meeting meeting) {
        if (meeting == null) {
            log.warn("Tentative de création d'un MeetingDTO complet à partir d'un Meeting null");
            return null;
        }

        try {
            MeetingDTO dto = new MeetingDTO();

            // Propriétés de base (toujours présentes)
            dto.setId(meeting.getId());
            dto.setMonth(meeting.getMonth());
            dto.setStatus(meeting.getStatus());
            dto.setYear(meeting.getYear());
            dto.setDate(meeting.getDate());
            dto.setTime(meeting.getTime());

            // Relations complètes (pour les pages de détail)
            dto.setAgendaItems(MeetingProjectDTO.fromEntityList(meeting.getAgendaItems()));
            dto.setParticipants(MeetingParticipantDTO.fromEntityList(meeting.getAttendees()));
            dto.setAttendances(MeetingAttendanceDTO.fromEntityList(meeting.getAttendances()));

            // Compteurs basés sur les listes chargées
            dto.setAgendaItemsCount(dto.getAgendaItems().size());
            dto.setParticipantsCount(dto.getParticipants().size());
            dto.setAttendancesCount(dto.getAttendances().size());

            return dto;
        } catch (Exception e) {
            log.error("Erreur lors de la création du MeetingDTO complet pour Meeting ID {}: {}",
                    meeting.getId(), e.getMessage(), e);
            return createSimple(meeting); // Fallback vers version simple
        }
    }

    /**
     * Version simplifiée pour les listes et calendriers
     * Inclut seulement les données essentielles pour optimiser les performances
     */
    public static MeetingDTO createSimple(Meeting meeting) {
        if (meeting == null) {
            log.warn("Tentative de création d'un MeetingDTO simple à partir d'un Meeting null");
            return null;
        }

        MeetingDTO dto = new MeetingDTO();

        // Propriétés de base seulement
        dto.setId(meeting.getId());
        dto.setMonth(meeting.getMonth());
        dto.setStatus(meeting.getStatus());
        dto.setYear(meeting.getYear());
        dto.setDate(meeting.getDate());
        dto.setTime(meeting.getTime());

        // Compteurs seulement (pas les listes complètes)
        dto.setAgendaItemsCount(meeting.getAgendaItems() != null ? meeting.getAgendaItems().size() : 0);
        dto.setParticipantsCount(meeting.getAttendees() != null ? meeting.getAttendees().size() : 0);
        dto.setAttendancesCount(meeting.getAttendances() != null ? meeting.getAttendances().size() : 0);

        // Listes vides pour éviter les erreurs côté frontend
        dto.setAgendaItems(List.of());
        dto.setParticipants(List.of());
        dto.setAttendances(List.of());

        return dto;
    }

    /**
     * Conversion de liste avec gestion d'erreurs
     */
    public static List<MeetingDTO> createSimpleList(List<Meeting> meetings) {
        if (meetings == null || meetings.isEmpty()) {
            return List.of();
        }

        return meetings.stream()
                .map(MeetingDTO::createSimple)
                .filter(dto -> dto != null) // Filtrer les conversions échouées
                .collect(Collectors.toList());
    }
}