package com.example.cerbo.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String civilite;
    private String nom;
    private String prenom;
    private String titre;
    private String laboratoire;
    private String affiliation;
}