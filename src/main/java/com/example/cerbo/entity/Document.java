package com.example.cerbo.entity;

import com.example.cerbo.entity.enums.DocumentType;
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
    private DocumentType type;

    private String name;
    private String path;
    private String contentType;
    private Long size;

    private LocalDateTime creationDate = LocalDateTime.now();
    private LocalDateTime modificationDate;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = true)
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
