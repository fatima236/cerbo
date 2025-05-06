package com.example.cerbo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class MeetingAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne
    @JoinColumn(name = "evaluator_id", nullable = false)
    private User evaluator;

    private boolean present;

    private String justification;
}