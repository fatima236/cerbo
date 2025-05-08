package com.example.cerbo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RemarkResponseDTO {
    private Long id;
    private String content;
    private LocalDateTime creationDate;
    private String adminStatus;
    private LocalDateTime validationDate;
    private ReviewerDTO reviewer;
    private String comment;
    private String adminResponse;
    private LocalDateTime adminResponseDate;

    @Data
    public static class ReviewerDTO {
        private String email;
        private String prenom;
        private String nom;
    }
}