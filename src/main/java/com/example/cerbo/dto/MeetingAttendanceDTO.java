package com.example.cerbo.dto;

import com.example.cerbo.entity.MeetingAttendance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingAttendanceDTO {
    private Long id;
    private Long evaluatorId;
    private String evaluatorName;
    private boolean present;
    private String justification;
    private boolean justified;

    public static MeetingAttendanceDTO fromEntity(MeetingAttendance attendance) {
        MeetingAttendanceDTO dto = new MeetingAttendanceDTO();
        dto.setId(attendance.getId());
        dto.setPresent(attendance.isPresent());
        dto.setJustification(attendance.getJustification());
        dto.setJustified(attendance.isJustified());

        if (attendance.getEvaluator() != null) {
            dto.setEvaluatorId(attendance.getEvaluator().getId());

            // ✅ SOLUTION TEMPORAIRE: Utiliser l'email ou l'ID
            dto.setEvaluatorName("Évaluateur ID: " + attendance.getEvaluator().getId());
        }

        return dto;
    }

    public static List<MeetingAttendanceDTO> fromEntityList(List<MeetingAttendance> attendances) {
        if (attendances == null) return List.of();
        return attendances.stream()
                .map(MeetingAttendanceDTO::fromEntity)
                .collect(Collectors.toList());
    }
}