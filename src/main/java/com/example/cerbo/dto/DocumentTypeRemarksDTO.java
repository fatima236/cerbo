package com.example.cerbo.dto;

import com.example.cerbo.entity.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DocumentTypeRemarksDTO {
    private DocumentType documentType;
    private List<DocumentInfoDTO> documents;
    private List<RemarkDTO> remarks;
}