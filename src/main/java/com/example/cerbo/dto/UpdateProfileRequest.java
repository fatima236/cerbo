package com.example.cerbo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateProfileRequest {
    private String civilite;
    private String nom;
    private String prenom;
    private String titre;
    private String laboratoire;
    private String affiliation;
    private MultipartFile photoFile;
}