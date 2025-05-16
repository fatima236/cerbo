package com.example.cerbo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor @NoArgsConstructor @Data
public class SignupRequest {
    private String email;
    private String password;
    private String role;
    private String civilite;
    private String nom;
    private String prenom;
    private String titre;
    private String laboratoire;
    private String affiliation;

}
