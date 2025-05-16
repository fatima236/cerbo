package com.example.cerbo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "resource_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path;

    private String contentType;
    private Long size;
    private String description;
    private LocalDateTime creationDate = LocalDateTime.now();

    // Champs facultatifs pour la catégorisation
    private String category;
    private String tags;
}