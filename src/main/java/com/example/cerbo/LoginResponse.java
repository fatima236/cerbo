package com.example.cerbo;

public class LoginResponse {
    private String email;
    private String role;

    public LoginResponse(String email, String role) {
        this.email = email;
        this.role = role;
    }

    // Getters et setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
