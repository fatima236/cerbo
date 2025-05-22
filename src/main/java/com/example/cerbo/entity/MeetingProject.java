package com.example.cerbo.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_projects", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"meeting_id", "project_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    @JsonBackReference("meeting-agendaItems")
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;
}