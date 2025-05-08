package com.example.cerbo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStatsDto {
    private long projects;
    private long pendingRequests;
    private long events;
    private long users;
    private long articles;
    private long trainings;
}
