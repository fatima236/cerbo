package com.example.cerbo.dto;

import lombok.Data;

@Data
public class RoleLoginRequest {
    private String email;
    private String password;
    private Long userId; // ID de l'utilisateur spécifique à utiliser
}