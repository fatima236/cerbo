package com.example.cerbo.entity;

import com.example.cerbo.entity.enums.RemarkStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class DocumentReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_email")
    private String adminEmail;

    @Column(name = "admin_validation_date")
    private LocalDateTime adminValidationDate;

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

    private RemarkStatus status;
    private String remark;
    private LocalDateTime reviewDate;

    @Column(nullable = false)
    private boolean finalized = false; // False = brouillon, True = soumission finale

    private LocalDateTime submissionDate; // Date de soumission finale
    @Column(nullable = false)
    private boolean finalSubmission = false;
}
