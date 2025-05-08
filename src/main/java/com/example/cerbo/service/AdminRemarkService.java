package com.example.cerbo.service;

import com.example.cerbo.dto.RemarkDTO;
import com.example.cerbo.dto.ReportPreview;
import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.RemarkRepository;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminRemarkService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ProjectRepository projectRepository;

    public ReportPreview generateReportPreview(Long projectId, List<Long> documentIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

        List<Document> documents = documentRepository.findAllById(documentIds).stream()
                .filter(d -> d.getProject().getId().equals(projectId))
                .filter(d -> d.getAdminStatus() == RemarkStatus.VALIDATED)
                .collect(Collectors.toList());

        List<RemarkDTO> remarkDTOs = documents.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new ReportPreview(
                projectId,
                project.getTitle(),
                remarkDTOs
        );
    }

    private RemarkDTO convertToDto(Document document) {
        RemarkDTO dto = new RemarkDTO();
        dto.setId(document.getId());
        dto.setContent(document.getReviewRemark());
        dto.setCreationDate(document.getReviewDate());
        dto.setAdminStatus(document.getAdminStatus() != null ? document.getAdminStatus().toString() : null);
        dto.setValidationDate(document.getAdminValidationDate());

        if (document.getReviewer() != null) {
            dto.setReviewerId(document.getReviewer().getId());
            dto.setReviewerName(document.getReviewer().getPrenom() + " " + document.getReviewer().getNom());
        }

        dto.setResponse(document.getAdminResponse());
        dto.setResponseDate(document.getAdminResponseDate());
        dto.setHasResponseFile(document.getResponseFilePath() != null);

        return dto;
    }
}