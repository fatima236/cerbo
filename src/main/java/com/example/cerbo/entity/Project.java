package com.example.cerbo.entity;

import com.example.cerbo.entity.enums.ProjectStatut;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "projets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String reference;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private LocalDateTime dateSoumission;

    @Enumerated(EnumType.STRING)
    private ProjectStatut etat = ProjectStatut.EN_COURS;

    private String dureeEtude;

    private String populationCible;
    private String typeConsentement;
    private Boolean prelevement;
    private String typePrelevement;
    private Integer quantitePrelevement;
    private String sourceFinancement;
    private String programmeFinancement;

    @ManyToOne
    @JoinColumn(name = "investigateur_id", nullable = false)
    private User investigateurPrincipal;

    @ManyToMany
    @JoinTable(
            name = "projet_evaluateurs",
            joinColumns = @JoinColumn(name = "projet_id"),
            inverseJoinColumns = @JoinColumn(name = "evaluateur_id")
    )
    private Set<User> evaluateurs = new HashSet<>();

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Remarque> remarques = new ArrayList<>();

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rapport> rapports = new ArrayList<>();
}