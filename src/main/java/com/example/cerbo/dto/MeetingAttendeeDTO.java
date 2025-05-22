package com.example.cerbo.dto;

import com.example.cerbo.entity.MeetingAttendee;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingAttendeeDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Boolean addedManually;
    private Long relatedProjectId;
    private String relatedProjectName;

    public static MeetingAttendeeDTO fromEntity(MeetingAttendee attendee) {
        MeetingAttendeeDTO dto = new MeetingAttendeeDTO();
        dto.setId(attendee.getId());
        dto.setAddedManually(attendee.getAddedManually());

        if (attendee.getUser() != null) {
            dto.setUserId(attendee.getUser().getId());


            // ✅ SOLUTION TEMPORAIRE: Utiliser l'email ou l'ID
            dto.setUserName("Utilisateur ID: " + attendee.getUser().getId());
            dto.setUserEmail(attendee.getUser().getEmail());
        }

        if (attendee.getRelatedProject() != null) {
            dto.setRelatedProjectId(attendee.getRelatedProject().getId());
            dto.setRelatedProjectName(attendee.getRelatedProject().getTitle()); // ✅ CORRIGÉ: getTitle() au lieu de getName()
        }

        return dto;
    }

    public static List<MeetingAttendeeDTO> fromEntityList(List<MeetingAttendee> attendees) {
        if (attendees == null) return List.of();
        return attendees.stream()
                .map(MeetingAttendeeDTO::fromEntity)
                .collect(Collectors.toList());
    }
}