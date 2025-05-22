package com.example.cerbo.dto;

import com.example.cerbo.entity.MeetingProject;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingProjectDTO {
    private Long id;
    private Long projectId;
    private String projectName;
    private String projectDescription;
    private Integer orderIndex;

    // Méthode de conversion depuis MeetingProject
    public static MeetingProjectDTO fromEntity(MeetingProject meetingProject) {
        MeetingProjectDTO dto = new MeetingProjectDTO();
        dto.setId(meetingProject.getId());
        dto.setOrderIndex(meetingProject.getOrderIndex());

        if (meetingProject.getProject() != null) {
            dto.setProjectId(meetingProject.getProject().getId());

            // ✅ SOLUTION FINALE: Utilisation des vrais champs de Project
            dto.setProjectName(meetingProject.getProject().getTitle());
            dto.setProjectDescription(meetingProject.getProject().getProjectDescription());
        }

        return dto;
    }

    public static List<MeetingProjectDTO> fromEntityList(List<MeetingProject> meetingProjects) {
        if (meetingProjects == null) return List.of();
        return meetingProjects.stream()
                .map(MeetingProjectDTO::fromEntity)
                .collect(Collectors.toList());
    }
}