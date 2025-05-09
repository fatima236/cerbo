package com.example.cerbo.controller;

import com.example.cerbo.dto.DocumentReviewDTO;
import com.example.cerbo.dto.RemarkResponseDTO;
import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.RemarkRepository;
import com.example.cerbo.service.AdminRemarkService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.cerbo.dto.ReportPreview;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/remarks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRemarkController {

    private final AdminRemarkService adminRemarkService;
    private final DocumentRepository documentRepository;

    @GetMapping("/pending")
    public ResponseEntity<List<RemarkResponseDTO>> getPendingRemarks() {
        List<Document> documents = documentRepository.findByReviewStatusAndReviewRemarkIsNotNull(RemarkStatus.REVIEWED);

        return ResponseEntity.ok(documents.stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<List<RemarkResponseDTO>> getProjectRemarks(@PathVariable Long projectId) {
        List<Document> documents = documentRepository.findByProjectIdAndReviewRemarkIsNotNull(projectId);
        List<DocumentReviewDTO> reviews = documents.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(documents.stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    @PutMapping("/{documentId}/status")
    public ResponseEntity<RemarkResponseDTO> updateRemarkStatus(
            @PathVariable Long documentId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String status = request.get("status");
        String adminEmail = authentication.getName();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        document.setAdminStatus(RemarkStatus.valueOf(status));
        document.setAdminValidationDate(LocalDateTime.now());
        document.setAdminEmail(adminEmail);

        Document updatedDoc = documentRepository.save(document);
        return ResponseEntity.ok(convertToDto(updatedDoc));
    }

    @GetMapping("/projects/{projectId}/validated")
    public ResponseEntity<List<RemarkResponseDTO>> getValidatedRemarks(@PathVariable Long projectId) {
        List<Document> documents = documentRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.VALIDATED);
        return ResponseEntity.ok(documents.stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    @PostMapping("/projects/{projectId}/generate-report")
    public ResponseEntity<ReportPreview> generateReportPreview(
            @PathVariable Long projectId,
            @RequestBody List<Long> documentIds) {

        ReportPreview preview = adminRemarkService.generateReportPreview(projectId, documentIds);
        return ResponseEntity.ok(preview);
    }

    @PutMapping("/{documentId}/content")
    public ResponseEntity<RemarkResponseDTO> updateRemarkContent(
            @PathVariable Long documentId,
            @RequestBody UpdateRemarkRequest request,
            Authentication authentication) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        document.setReviewRemark(request.getContent());
        document.setAdminComment(request.getComment());
        document.setAdminResponse(request.getAdminResponse());
        document.setAdminResponseDate(LocalDateTime.now());
        document.setAdminEmail(authentication.getName());

        Document updatedDoc = documentRepository.save(document);
        return ResponseEntity.ok(convertToDto(updatedDoc));
    }

    private RemarkResponseDTO convertToDto(Document document) {
        RemarkResponseDTO dto = new RemarkResponseDTO();
        dto.setId(document.getId());
        dto.setContent(document.getReviewRemark());
        dto.setCreationDate(document.getReviewDate());
        dto.setAdminStatus(document.getAdminStatus() != null ? document.getAdminStatus().name() : null);
        dto.setValidationDate(document.getAdminValidationDate());
        dto.setComment(document.getAdminComment());
        dto.setAdminResponse(document.getAdminResponse());
        dto.setAdminResponseDate(document.getAdminResponseDate());

        if (document.getReviewer() != null) {
            RemarkResponseDTO.ReviewerDTO reviewerDto = new RemarkResponseDTO.ReviewerDTO();
            reviewerDto.setEmail(document.getReviewer().getEmail());
            reviewerDto.setPrenom(document.getReviewer().getPrenom());
            reviewerDto.setNom(document.getReviewer().getNom());
            dto.setReviewer(reviewerDto);
        }

        return dto;
    }

    private DocumentReviewDTO convertToDTO(Document document) {
        DocumentReviewDTO dto = new DocumentReviewDTO();
        dto.setId(document.getId());
        dto.setName(document.getName());
        dto.setReviewStatus(document.getReviewStatus());
        dto.setReviewRemark(document.getReviewRemark());
        dto.setReviewDate(document.getReviewDate());
        if (document.getReviewer() != null) {
            dto.setReviewerId(document.getReviewer().getId());
            dto.setReviewerNom(document.getReviewer().getNom());
            dto.setReviewerPrenom(document.getReviewer().getPrenom());
            dto.setReviewerEmail(document.getReviewer().getEmail());

        }

        if (document.getProject() != null) {
            dto.setProjectId(document.getProject().getId());
            dto.setProjectTitle(document.getProject().getTitle());
        }

        dto.setDocumentName(document.getName());
        dto.setDocumentType(document.getType().name());
        return dto;
    }
}

@Data
class UpdateRemarkRequest {
    private String content;
    private String comment;
    private String adminResponse;
}