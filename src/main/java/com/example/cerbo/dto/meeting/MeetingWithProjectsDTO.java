package com.example.cerbo.dto.meeting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingWithProjectsDTO {
    private Long id;
    private LocalDate date;
    private LocalTime time;
    private String status;
    private List<ProjectDTO> projects;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectDTO {
        private Long id;
        private String title;
        private String reference;
    }
}
