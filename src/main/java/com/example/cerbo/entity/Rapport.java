package com.example.cerbo.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "rapports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rapport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "rapport_id")
    private List<Remarque> remarques;

    private LocalDateTime dateCreation;



    @ManyToOne
    @JoinColumn(name = "projet_id", nullable = false)
    private Project project;

    private String nomFichier;
    private String chemin;
}