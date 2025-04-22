package com.example.cerbo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

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
    @Column(name = "roles")
    private Set<String> roles;
    private boolean validated = false;
    private String photoUrl; // URL de la photo stockée





    @Transient // Ne sera pas persisté en base
    private MultipartFile photoFile; // Pour la réception du fichier
    // Méthode pour vérifier si le profil est complet

    // Méthode pratique pour obtenir le nom complet

}