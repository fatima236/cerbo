package com.example.cerbo.dto;

import com.example.cerbo.entity.enums.DocumentType;
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
    private String response;
    private String comment;
    private String adminResponse;
    private LocalDateTime adminResponseDate;
    private String documentName;
    private DocumentType documentType;

    @Data
    public static class ReviewerDTO {
        private String email;
        private String prenom;
        private String nom;
    }
}