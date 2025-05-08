package com.example.cerbo.dto;

import com.example.cerbo.entity.enums.RemarkStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentReviewDTO {
    private Long id;
    private String name;
    private RemarkStatus reviewStatus;
    private String reviewRemark;
    private LocalDateTime reviewDate;
    private Long reviewerId;
    private String reviewerName;
    private String documentName;
    private String projectTitle;
    private String projectDescription;

}
