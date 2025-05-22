package com.example.cerbo.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Data
@Entity
public class MeetingAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "meeting_id", nullable = false)
    @JsonBackReference("meeting-attendances")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Meeting meeting;

    @ManyToOne
    @JoinColumn(name = "evaluator_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User evaluator;

    private boolean present;
    private String justification;
    private boolean justified = false;
}