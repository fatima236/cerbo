package com.example.cerbo.dto;

import lombok.Data;

@Data
public class DocumentReviewRequest {
    private boolean validated;
    private String remark;
}
