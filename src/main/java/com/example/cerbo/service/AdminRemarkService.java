package com.example.cerbo.service;

import com.example.cerbo.dto.RemarkDTO;
import com.example.cerbo.dto.ReportPreview;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
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

    private final RemarkRepository remarkRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ProjectRepository projectRepository;

    public List<Remark> getPendingRemarks() {
        return remarkRepository.findByAdminStatus(RemarkStatus.PENDING);
    }
    public Remark updateRemarkContent(Long remarkId, String content, String comment) {
        Remark remark = remarkRepository.findById(remarkId)
                .orElseThrow(() -> new ResourceNotFoundException("Remarque non trouvée"));

        if (content != null) {
            remark.setContent(content);
        }

        if (comment != null) {
            remark.setComment(comment);
        }

        return remarkRepository.save(remark);
    }

    @Transactional
    public Remark updateRemarkStatus(Long remarkId, String status, String adminEmail) {
        Remark remark = remarkRepository.findById(remarkId)
                .orElseThrow(() -> new ResourceNotFoundException("Remarque non trouvée"));

        User admin = userRepository.findByEmail(adminEmail);
        RemarkStatus newStatus = RemarkStatus.valueOf(status);

        remark.setAdminStatus(RemarkStatus.valueOf(status));
        remark.setValidationDate(LocalDateTime.now());
        remark.setValidatedBy(admin);

        // Notifier l'évaluateur si la remarque est validée ou rejetée
        if (newStatus == RemarkStatus.VALIDATED || newStatus == RemarkStatus.REJECTED) {
            notifyReviewer(remark, newStatus);
        }


        return remarkRepository.save(remark);
    }

    public List<Remark> getValidatedRemarks(Long projectId) {
        return remarkRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.VALIDATED);
    }
    public ReportPreview generateReportPreview(Long projectId, List<Long> remarkIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

        List<Remark> remarks = remarkRepository.findAllById(remarkIds).stream()
                .filter(r -> r.getProject().getId().equals(projectId))
                .filter(r -> r.getAdminStatus() == RemarkStatus.VALIDATED)
                .collect(Collectors.toList());

        // Convertir les Remark en RemarkDTO
        List<RemarkDTO> remarkDTOs = remarks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new ReportPreview(
                projectId,
                project.getTitle(),
                remarkDTOs
        );
    }

    private RemarkDTO convertToDto(Remark remark) {
        RemarkDTO dto = new RemarkDTO();

        // Champs de base
        dto.setId(remark.getId());
        dto.setContent(remark.getContent());
        dto.setCreationDate(remark.getCreationDate());

        // Statut admin
        dto.setAdminStatus(remark.getAdminStatus() != null ?
                remark.getAdminStatus().toString() : null);
        dto.setValidationDate(remark.getValidationDate());

        // Informations réponse
        dto.setResponse(remark.getResponse());
        dto.setResponseDate(remark.getResponseDate());
        dto.setHasResponseFile(remark.getResponseFilePath() != null);

        // Informations évaluateur
        if (remark.getReviewer() != null) {
            dto.setReviewerId(remark.getReviewer().getId());
            dto.setReviewerName(
                    remark.getReviewer().getPrenom() + " " +
                            remark.getReviewer().getNom()
            );
        }

        // Validateur (admin)
        if (remark.getValidatedBy() != null) {
            dto.setValidatorName(
                    remark.getValidatedBy().getPrenom() + " " +
                            remark.getValidatedBy().getNom()
            );
        }

        return dto;
    }

    private void notifyReviewer(Remark remark, RemarkStatus status) {
        if (remark.getReviewer() != null) {
            String message = String.format(
                    "Votre remarque sur le projet %s a été %s par l'admin",
                    remark.getProject().getTitle(),
                    status == RemarkStatus.VALIDATED ? "validée" : "rejetée"
            );
            notificationService.createNotification(
                    remark.getReviewer().getEmail(),
                    message
            );
        }
    }
}