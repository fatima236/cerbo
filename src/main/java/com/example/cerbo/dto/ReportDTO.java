package com.example.cerbo.dto;

import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.enums.ReportStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

public class ReportDTO {
    private Long id;
    private Long projectId;
    private String projectTitle;
    private LocalDateTime creationDate;
    private ReportStatus status;
    private String fileName;
    private String filePath;
    private LocalDateTime sentDate;
    private LocalDateTime responseDeadline;
    private boolean responded;
    private LocalDateTime responseDate;
    private List<DocumentReviewDTO> includedReviews;


}

