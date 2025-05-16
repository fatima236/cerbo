package com.example.cerbo.service;

import com.example.cerbo.annotation.Loggable;
import com.example.cerbo.dto.*;
import com.example.cerbo.entity.*;
import com.example.cerbo.entity.enums.DocumentType;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminRemarkService {

    private final DocumentRepository documentRepository;
    private final DocumentReviewRepository documentReviewRepository;

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
    public OrganizedRemarksDTO getOrganizedRemarks(Long projectId) {
        // Récupérer toutes les évaluations finalisées
        List<DocumentReview> evaluations = documentReviewRepository
                .findByDocumentProjectIdAndFinalizedTrue(projectId)
                .stream()
                .filter(review -> review.getRemark() != null && !review.getRemark().isEmpty())
                .collect(Collectors.toList());

        // Convertir en DTO
        List<RemarkResponseDTO> allRemarks = evaluations.stream()
                .map(this::convertReviewToRemarkResponseDTO)
                .collect(Collectors.toList());

        OrganizedRemarksDTO dto = new OrganizedRemarksDTO();

        // 1. Organisation par type de document (avec ordre spécifique)
        Map<DocumentType, List<RemarkResponseDTO>> byDocType = evaluations.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getDocument().getType(),
                        Collectors.mapping(this::convertReviewToRemarkResponseDTO, Collectors.toList())
                ));

        // Créer un LinkedHashMap pour préserver l'ordre
        Map<DocumentType, List<RemarkResponseDTO>> orderedByDocType = new LinkedHashMap<>();

        // Ajouter les types dans l'ordre souhaité
        for (DocumentType type : DocumentType.values()) {
            if (byDocType.containsKey(type)) {
                orderedByDocType.put(type, byDocType.get(type));
            }
        }

        dto.setByDocumentType(orderedByDocType);

        // 2. Organisation par évaluateur (uniquement les remarques avec contenu)
        dto.setByReviewer(allRemarks.stream()
                .filter(r -> r.getContent() != null && !r.getContent().isEmpty())
                .collect(Collectors.groupingBy(
                        r -> r.getReviewer().getPrenom() + " " + r.getReviewer().getNom(),
                        TreeMap::new, // Trie par nom
                        Collectors.toList()
                )));

        // 3. Organisation par statut
        Map<String, List<RemarkResponseDTO>> byStatus = evaluations.stream()
                .map(this::convertReviewToRemarkResponseDTO)
                .collect(Collectors.groupingBy(
                        r -> r.getAdminStatus() != null ? r.getAdminStatus() : "PENDING",
                        () -> new TreeMap<>(Comparator.comparingInt(this::getStatusOrder)),
                        Collectors.toList()
                ));

        dto.setByStatus(byStatus);

        // 4. Statistiques
        RemarkStatisticsDTO stats = new RemarkStatisticsDTO();
        stats.setTotalRemarks(allRemarks.size());
        stats.setPendingCount(allRemarks.stream().filter(r -> r.getAdminStatus() == null || "PENDING".equals(r.getAdminStatus())).count());
        stats.setValidatedCount(allRemarks.stream().filter(r -> "VALIDATED".equals(r.getAdminStatus())).count());
        stats.setRejectedCount(allRemarks.stream().filter(r -> "REJECTED".equals(r.getAdminStatus())).count());

        stats.setCountByDocumentType(allRemarks.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getDocumentType(),
                        Collectors.counting()
                )));

        stats.setCountByReviewer(allRemarks.stream()
                .filter(r -> r.getReviewer() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getReviewer().getPrenom() + " " + r.getReviewer().getNom(),
                        Collectors.counting()
                )));

        dto.setStatistics(stats);

        return dto;
    }

    private int getStatusOrder(String status) {
        return switch (status) {
            case "PENDING" -> 1;
            case "VALIDATED" -> 2;
            case "REJECTED" -> 3;
            default -> 99;
        };
    }    private RemarkResponseDTO convertReviewToRemarkResponseDTO(DocumentReview review) {
        RemarkResponseDTO dto = new RemarkResponseDTO();
        dto.setId(review.getId());
        dto.setContent(review.getRemark());
        dto.setCreationDate(review.getReviewDate());
        dto.setAdminStatus(review.getStatus() != null ? review.getStatus().name() : "PENDING");

        if (review.getReviewer() != null) {
            RemarkResponseDTO.ReviewerDTO reviewerDto = new RemarkResponseDTO.ReviewerDTO();
            reviewerDto.setEmail(review.getReviewer().getEmail());
            reviewerDto.setPrenom(review.getReviewer().getPrenom());
            reviewerDto.setNom(review.getReviewer().getNom());
            dto.setReviewer(reviewerDto);
        }

        if (review.getDocument() != null) {
            dto.setDocumentName(review.getDocument().getName());
            dto.setDocumentType(review.getDocument().getType());
        }

        return dto;
    }

    private RemarkResponseDTO convertToRemarkResponseDTO(Document document) {
        RemarkResponseDTO dto = new RemarkResponseDTO();
        dto.setId(document.getId());
        dto.setContent(document.getReviewRemark());
        dto.setCreationDate(document.getReviewDate());
        dto.setAdminStatus(document.getAdminStatus() != null ? document.getAdminStatus().name() : null);
        dto.setValidationDate(document.getAdminValidationDate());
        dto.setComment(document.getAdminComment());
        dto.setAdminResponse(document.getAdminResponse());
        dto.setAdminResponseDate(document.getAdminResponseDate());


        // Info reviewer
        if (document.getReviewer() != null) {
            RemarkResponseDTO.ReviewerDTO reviewerDto = new RemarkResponseDTO.ReviewerDTO();
            reviewerDto.setEmail(document.getReviewer().getEmail());
            reviewerDto.setPrenom(document.getReviewer().getPrenom());
            reviewerDto.setNom(document.getReviewer().getNom());
            dto.setReviewer(reviewerDto);
        }

        // Info document
        dto.setDocumentName(document.getName());
        dto.setDocumentType(document.getType());

        return dto;
    }

    public List<RemarkResponseDTO> getValidatedRemarksForReport(Long projectId) {
        return documentRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.VALIDATED).stream()
                .filter(d -> d.getReviewRemark() != null && !d.getReviewRemark().isEmpty())
                .map(this::convertToRemarkResponseDTO)
                .collect(Collectors.toList());
    }

    public List<RemarkResponseDTO> getRejectedRemarks(Long projectId) {
        return documentRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.REJECTED).stream()
                .map(this::convertToRemarkResponseDTO)
                .collect(Collectors.toList());
    }
}