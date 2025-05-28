package com.example.cerbo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Column(nullable = false, columnDefinition = "boolean default true") // Changez à true si vous voulez que ce soit activé par défaut
    private boolean useAI = true;
    public String getFullName() {
        return (prenom != null ? prenom + " " : "") + (nom != null ? nom : "");
    }

    public String getEmail() {
        return email;
    }



    @Transient // Ne sera pas persisté en base
    private MultipartFile photoFile; // Pour la réception du fichier
    // Méthode pour vérifier si le profil est complet
    public boolean getUseAI() {
        return useAI;
    }

    public void setUseAI(boolean useAI) {
        this.useAI = useAI;
    }
    // Ajoutez soit cette méthode (standard pour les booléens)
    public boolean isUseAI() {
        return useAI;
    }

    // Méthode pratique pour obtenir le nom complet

}