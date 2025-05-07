package com.example.cerbo.entity;

import com.example.cerbo.entity.enums.RemarkStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Entity
@Table(name = "remarks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Remark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private LocalDateTime creationDate;

    @Column(nullable = false)
    private boolean includedInReport = false;

    @Column(columnDefinition = "TEXT")
    private String adminComment; // Commentaire de l'admin

    @ManyToOne
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    private String comment;
    private LocalDateTime createdAt;

    private String response;
    private String responseFilePath;
    private LocalDateTime responseDate;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Nouveaux champs
    @Enumerated(EnumType.STRING)
    private RemarkStatus adminStatus = RemarkStatus.PENDING;

    private LocalDateTime validationDate;

    @ManyToOne
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    public boolean isIncludedInReport() {
        return includedInReport && adminStatus == RemarkStatus.VALIDATED;
    }
}

