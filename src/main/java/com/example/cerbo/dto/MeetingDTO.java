package com.example.cerbo.dto;

import com.example.cerbo.entity.Meeting;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDTO {
    private Long id;
    private String month;
    private String status;
    private int year;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime time;

    // ✅ NOUVELLES PROPRIÉTÉS : Relations complètes via DTOs
    private List<MeetingProjectDTO> agendaItems;
    private List<MeetingAttendeeDTO> attendees;
    private List<MeetingAttendanceDTO> attendances;

    // ✅ CONSERVER : Compteurs pour compatibilité
    private int agendaItemsCount;
    private int attendeesCount;
    private int attendancesCount;

    /**
     * Convertit une entité Meeting en DTO complet
     */
    public static MeetingDTO fromEntity(Meeting meeting) {
        MeetingDTO dto = new MeetingDTO();
        dto.setId(meeting.getId());
        dto.setMonth(meeting.getMonth());
        dto.setStatus(meeting.getStatus());
        dto.setYear(meeting.getYear());
        dto.setDate(meeting.getDate());
        dto.setTime(meeting.getTime());

        // ✅ CONVERSION DES RELATIONS EN DTOs
        dto.setAgendaItems(MeetingProjectDTO.fromEntityList(meeting.getAgendaItems()));
        dto.setAttendees(MeetingAttendeeDTO.fromEntityList(meeting.getAttendees()));
        dto.setAttendances(MeetingAttendanceDTO.fromEntityList(meeting.getAttendances()));

        // ✅ COMPTEURS POUR COMPATIBILITÉ
        dto.setAgendaItemsCount(dto.getAgendaItems().size());
        dto.setAttendeesCount(dto.getAttendees().size());
        dto.setAttendancesCount(dto.getAttendances().size());

        return dto;
    }

    /**
     * Version simplifiée sans relations (pour les listes)
     */
    public static MeetingDTO fromEntitySimple(Meeting meeting) {
        MeetingDTO dto = new MeetingDTO();
        dto.setId(meeting.getId());
        dto.setMonth(meeting.getMonth());
        dto.setStatus(meeting.getStatus());
        dto.setYear(meeting.getYear());
        dto.setDate(meeting.getDate());
        dto.setTime(meeting.getTime());

        // ✅ SEULEMENT LES COMPTEURS (pas les listes complètes)
        dto.setAgendaItemsCount(meeting.getAgendaItems() != null ? meeting.getAgendaItems().size() : 0);
        dto.setAttendeesCount(meeting.getAttendees() != null ? meeting.getAttendees().size() : 0);
        dto.setAttendancesCount(meeting.getAttendances() != null ? meeting.getAttendances().size() : 0);

        // ✅ LISTES VIDES POUR ÉVITER LES ERREURS
        dto.setAgendaItems(List.of());
        dto.setAttendees(List.of());
        dto.setAttendances(List.of());

        return dto;
    }

    /**
     * Convertit une liste d'entités Meeting en liste de DTOs
     */
    public static List<MeetingDTO> fromEntityList(List<Meeting> meetings) {
        if (meetings == null) return List.of();
        return meetings.stream()
                .map(MeetingDTO::fromEntitySimple) // Utiliser la version simple pour les listes
                .collect(Collectors.toList());
    }

    /**
     * Convertit le DTO vers une entité (pour les opérations de mise à jour)
     */
    public Meeting toEntity() {
        Meeting meeting = new Meeting();
        meeting.setId(this.id);
        meeting.setMonth(this.month);
        meeting.setStatus(this.status);
        meeting.setYear(this.year);
        meeting.setDate(this.date);
        meeting.setTime(this.time);
        return meeting;
    }
}