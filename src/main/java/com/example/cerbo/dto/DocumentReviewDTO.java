package com.example.cerbo.dto;

import com.example.cerbo.entity.enums.DocumentType;
import com.example.cerbo.entity.enums.RemarkStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DocumentReviewDTO {
    private Long id;
    private Long documentId;
    private String name;
    private RemarkStatus reviewStatus;
    private String reviewRemark;
    private LocalDateTime reviewDate;
    private Long reviewerId;
    private String reviewerNom;
    private String reviewerPrenom;
    private String reviewerEmail;
    private String documentName;
    private String documentType;
    private Long projectId;
    private String projectTitle;
    private String projectDescription;

    private boolean finalized;
    private LocalDateTime submissionDate;

    private String adminComment;
    private String adminResponse;
    private LocalDateTime adminResponseDate;

}
