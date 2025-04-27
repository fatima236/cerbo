package com.example.cerbo.dto;

import com.example.cerbo.entity.enums.DocumentType;
import lombok.Data;

@Data
public class DocumentDTO {
    private Long id;
    private DocumentType type;
    private String name;
    private String downloadUrl;
    private Long size;
}