package com.example.cerbo.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "meeting_attendees", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"meeting_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingAttendee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    @JsonBackReference("meeting-attendees")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "added_manually")
    private Boolean addedManually = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_project_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Project relatedProject;

}