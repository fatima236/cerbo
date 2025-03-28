package com.example.cerbo.entity;


import com.example.cerbo.entity.enums.TypeDocument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TypeDocument type;

    private String nom;
    private String chemin;
    private String contentType;
    private Long taille;

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project projet;

    @ManyToOne
    @JoinColumn(name = "formation_id")
    private Formation formation;

    @ManyToOne
    @JoinColumn(name = "article_id")
    private Article article;

    @ManyToOne
    @JoinColumn(name = "evenement_id")
    private Event evenement;
}