package com.example.cerbo.controller;

import com.example.cerbo.dto.DocumentReviewDTO;
import com.example.cerbo.service.NotificationService;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.io.IOException;
import java.nio.file.Path;

import com.example.cerbo.entity.*;
import com.example.cerbo.dto.RemarkResponseDTO;
import com.example.cerbo.entity.enums.DocumentType;
import com.example.cerbo.repository.*;
import com.example.cerbo.service.ProjectService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.example.cerbo.dto.RemarkDTO;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.entity.User;
import com.example.cerbo.service.RemarkReportService;
import com.example.cerbo.service.reportService.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.cerbo.repository.DocumentReviewRepository;
import org.springframework.web.bind.annotation.*;
import com.example.cerbo.entity.enums.ReportStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/projects/{projectId}/report")
@RequiredArgsConstructor
public class RemarkReportController {

    private final RemarkReportService remarkReportService;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final DocumentReviewRepository documentReviewRepository;
    private final ReportService reportService;
    private final RemarkRepository remarkRepository;
    private final ReportRepository reportRepository;
    private final NotificationService notificationService;

    private DocumentReviewDTO convertToDTO(DocumentReview documentReview) {
        DocumentReviewDTO dto = new DocumentReviewDTO();
        dto.setId(documentReview.getId());
        dto.setReviewerNom(documentReview.getReviewer().getNom());
        dto.setStatus(documentReview.getStatus());
        dto.setContent(documentReview.getContent());
        dto.setCreationDate(documentReview.getCreationDate());
        dto.setResponse(documentReview.getResponse());
        if (documentReview.getReviewer() != null) {
            dto.setReviewerId(documentReview.getReviewer().getId());
            dto.setReviewerNom(documentReview.getReviewer().getNom());
            dto.setReviewerPrenom(documentReview.getReviewer().getPrenom());
            dto.setReviewerEmail(documentReview.getReviewer().getEmail());

        }

        if (documentReview.getReport().getProject() != null) {
              dto.setProjectId(documentReview.getReport().getProject().getId());
              dto.setProjectTitle(documentReview.getReport().getProject().getTitle());
       }

        dto.setDocumentName(documentReview.getReviewer().getFullName());



        return dto;
    }


    @PostMapping("/preview")
    public ResponseEntity<List<DocumentReviewDTO>> generateReportPreview(
            @PathVariable Long projectId,
            @RequestBody List<Long> documentIds) {

        List<DocumentReview > documentReviews = documentReviewRepository.findValidatedRemarksByProjectId(projectId);
        return ResponseEntity.ok(documentReviews.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendReportToInvestigator(@PathVariable Long projectId) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            Report report = reportService.finalizeAndSendReport(project.getLatestReport().getId());


            return ResponseEntity.ok(report);

        }
        catch (Exception e){
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la génération du rapport: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/genered")
    public ResponseEntity<Report> generedReport(@PathVariable Long projectId) {
        Project project =projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.badRequest().body(null);
        }
        List<Long> documentReview = documentReviewRepository.documentReviewValidated(projectId);
        Report report = reportService.createReport(projectId, documentReview);

        return ResponseEntity.ok(report);
    }












}