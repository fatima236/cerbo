package com.example.cerbo.entity;

import com.example.cerbo.entity.enums.EvaluationStatus;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.*;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String reference;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime submissionDate;

    @Column(name = "response_deadline")
    private LocalDateTime responseDeadline;

    private LocalDateTime reviewDate;
    private LocalDateTime decisionDate;

    @Column(nullable = false)
    private LocalDate reviewDeadline = LocalDate.now().plusDays(60);

    @Enumerated(EnumType.STRING)
    @Column(length = 50) // Adjust length as needed
    private EvaluationStatus evaluationStatus;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.SOUMIS; // Changé de EN_COURS à SOUMIS


    private LocalDateTime evaluationSubmitDate;
    private LocalDateTime LastReportDate;


    private String studyDuration;
    private String targetPopulation;
    private String consentType;
    private Boolean sampling;
    private String sampleType;
    private String sampleQuantity; // Changé de Integer à String pour plus de flexibilité
    private String fundingSource;
    private String fundingProgram;
    private String Description;



    @Column(columnDefinition = "TEXT")
    private String projectDescription;

    @Column(columnDefinition = "TEXT")
    private String ethicalConsiderations;


    @ManyToOne
    @JoinColumn(name = "principal_investigator_id", nullable = false)
    private User principalInvestigator;

    // Ajout des co-investigateurs
    @ManyToMany
    @JoinTable(
            name = "project_investigators",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "investigator_id")
    )
    private Set<User> investigators = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_reviewers",
            joinColumns = @JoinColumn(name = "project_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "reviewer_id", referencedColumnName = "id")
    )
    private Set<User> reviewers = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Remark> remarks = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Report> reports = new ArrayList<>();

    // Méthode pour générer la référence automatiquement
    @PrePersist
    public void prePersist() {
        if (this.reference == null) {
            this.reference = "PROJ-" + LocalDateTime.now().getYear() + "-" +
                    String.format("%06d", (int)(Math.random() * 1000000));
        }

        if (this.submissionDate == null) {
            this.submissionDate = LocalDateTime.now();
        }

        if (this.reviewDeadline == null) {
            this.reviewDeadline = LocalDate.now().plusDays(60);
        }
    }


}