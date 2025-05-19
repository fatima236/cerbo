package com.example.cerbo.entity;

import com.example.cerbo.entity.enums.RemarkStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class DocumentReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_email")
    private String adminEmail;

    @Column(name = "admin_validation_date")
    private LocalDateTime adminValidationDate;

    private LocalDateTime creationDate;

    @Column(name = "admin_response")
    private String adminResponse;

    @Column(name = "admin_response_date")
    private LocalDateTime adminResponseDate;

    @Column(name = "admin_comment")
    private String adminComment;

    @ManyToOne
    private Document document;

    @ManyToOne
    private User reviewer;

    @Enumerated(EnumType.STRING)
    private RemarkStatus status = RemarkStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "validated_by")
    private User validatedBy;


    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime reviewDate;

    @Column(columnDefinition = "TEXT")
    private String response;

    private LocalDateTime responseDate;
    private String responseFilePath;

    private boolean includedInReport = false;

    private boolean validated = false;

    private String raisonOfRejection;


    @Column(nullable = false)
    private boolean finalized = false; // False = brouillon, True = soumission finale

    private LocalDateTime submissionDate; // Date de soumission finale


    @ManyToOne
    @JoinColumn
    private Project project;

    private Boolean final_submission = false;

    @ManyToOne
    @JoinColumn(name="report_id")
    private Report report;
}
