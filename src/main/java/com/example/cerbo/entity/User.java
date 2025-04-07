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

    private String firstName;
    private String lastName;
    private String phone;
    private String bio; // Description du métier/statut (doctorant, docteur, professeur, etc.)
    private String photo;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles;
    private boolean validated = false;



    // Méthode pratique pour obtenir le nom complet
    public String getFullName() {
        return firstName + " " + lastName;
    }
}