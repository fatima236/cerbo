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
    private String documentName;
    private DocumentType documentType;
    private Long reviewerId;
    private String content;
    private LocalDateTime creationDate;
    private RemarkStatus  status;
    private String reviewerNom;
    private String reviewerPrenom;
    private String reviewerEmail;
    private LocalDateTime validationDate;
    private Long validatedById;
    private String validatedByName;
    private String response;
    private LocalDateTime responseDate;
    private Boolean hasResponseFile;
    private String adminComent;
    private boolean finalized;
    private LocalDateTime finalizedDate;
    private Long projectId;
    private String projectTitle;


}
