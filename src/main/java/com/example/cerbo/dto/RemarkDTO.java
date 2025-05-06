package com.example.cerbo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RemarkDTO {
    private Long id;
    private String content;
    private LocalDateTime creationDate;
    private Long reviewerId;
    private String reviewerName;

    private String adminStatus;
    private LocalDateTime validationDate;

    private String response;
    private LocalDateTime responseDate;
    private Boolean hasResponseFile;
}