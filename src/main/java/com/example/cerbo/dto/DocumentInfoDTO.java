package com.example.cerbo.dto;

import com.example.cerbo.entity.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentInfoDTO {
    private Long id;
    private String name;
    private DocumentType type;


}
