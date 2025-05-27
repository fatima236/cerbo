package com.example.cerbo.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private Long projectCount;
    private boolean useAI;
}
