package com.example.cerbo.entity;

import com.example.cerbo.entity.enums.DocumentType;
import com.example.cerbo.entity.enums.RemarkStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 70, nullable = false)
    private DocumentType type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path;
    private String contentType;
    private Long size;
    @Column(columnDefinition = "TEXT")
    private String remark;

    private boolean submitted;
    private boolean validated;


    private LocalDateTime creationDate = LocalDateTime.now();
    private LocalDateTime modificationDate;
    private LocalDateTime validationDate;
    private LocalDateTime reviewDate;

    @Enumerated(EnumType.STRING)
    private RemarkStatus reviewStatus;

    @Column(columnDefinition = "TEXT")
    private String reviewRemark;

    @ManyToOne
    private User reviewer;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = true) // Changé de nullable=true à false
    private Project project;

    @ManyToOne
    @JoinColumn(name = "training_id",nullable = true)
    private Training training;

    @ManyToOne
    @JoinColumn(name = "article_id",nullable = true)
    private Article article;

    @ManyToOne
    @JoinColumn(name = "event_id",nullable = true)
    private Event event;


}

