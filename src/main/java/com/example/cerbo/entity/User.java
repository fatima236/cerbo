package com.example.cerbo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

@Entity
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private String nom;
    private String prenom;
    // Nouveaux champs
    private String civilite; // Mr, Mme, etc.

    private String titre;
    private String laboratoire;
    private String affiliation;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles;
    private boolean validated = false;



    // Méthode pratique pour obtenir le nom complet

}