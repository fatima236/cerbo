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


    public OrganizedRemarksDTO getOrganizedRemarks(Long projectId) {
        // Récupérer toutes les évaluations finalisées
        List<DocumentReview> evaluations = documentReviewRepository
                .findValidatedUnreportedRemarks(projectId,RemarkStatus.VALIDATED)
                .stream()
                .filter(review -> review.getContent() != null && !review.getContent().isEmpty())
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
    }
    private RemarkResponseDTO convertReviewToRemarkResponseDTO(DocumentReview review) {
        RemarkResponseDTO dto = new RemarkResponseDTO();
        dto.setId(review.getId());
        dto.setContent(review.getContent());
        dto.setCreationDate(review.getReviewDate());
        dto.setAdminStatus(review.getStatus() != null ? review.getStatus().name() : "PENDING");
        dto.setResponse(review.getResponse());

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


}