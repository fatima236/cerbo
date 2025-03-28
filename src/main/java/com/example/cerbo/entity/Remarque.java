package com.example.cerbo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "remarques")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Remarque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    private LocalDateTime dateCreation;


    @ManyToOne
    @JoinColumn(name = "evaluateur_id", nullable = false)
    private User evaluateur;

    @ManyToOne
    @JoinColumn(name = "projet_id", nullable = false)
    private Project projet;
}