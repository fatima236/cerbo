package com.example.cerbo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RemarkDTO {
    private Long id;
    private String content;
    private LocalDateTime creationDate;
    private Long reviewerId;
    private String reviewerEmail;
    private String reviewerFirstName;
    private String reviewerName;

    private String adminStatus;
    private LocalDateTime validationDate;

    private String response;
    private LocalDateTime responseDate;
    private Boolean hasResponseFile;

    private String validatorName;
    private Long validatorId;

    private String comment;
    private String adminEmail;

    private LocalDateTime adminResponseDate;
    private String adminResponse;

    // Info projet
    private Long projectId;
    private String projectTitle;

    // Info document
    private Long documentId;
    private String documentName;

}