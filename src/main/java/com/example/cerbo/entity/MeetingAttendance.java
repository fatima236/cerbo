package com.example.cerbo.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(
        name = "meeting_attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"meeting_id", "evaluator_id"},
                        name = "unique_attendance_per_meeting"
                )
        }
)
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
    private boolean isManual = false;
    @Column(name = "manual_member")
    private String manualMember;

    // Historique des modifications
    @ElementCollection
    @CollectionTable(
            name = "attendance_history",
            joinColumns = @JoinColumn(name = "attendance_id")
    )
    private List<AttendanceHistoryEntry> history = new ArrayList<>();

    // Méthode pour enregistrer une modification
    public void recordChange(boolean newPresentStatus, String newJustification, boolean newJustified) {
        AttendanceHistoryEntry entry = new AttendanceHistoryEntry(
                this.present,
                this.justification,
                this.justified,
                LocalDateTime.now()
        );
        this.history.add(entry);

        this.present = newPresentStatus;
        this.justification = newJustification;
        this.justified = newJustified;
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceHistoryEntry {
        private boolean present;
        private String justification;
        private boolean justified;
        private LocalDateTime changeDate;
    }
}