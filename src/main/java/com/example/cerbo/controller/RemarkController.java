package com.example.cerbo.controller;

import com.example.cerbo.dto.RemarkDTO;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.RemarkRepository;
import com.example.cerbo.service.RemarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects/{projectId}/remarks")
@RequiredArgsConstructor
public class RemarkController {

    private final RemarkRepository remarkRepository;
    private final ProjectRepository projectRepository;
    private final RemarkService remarkService;

    @GetMapping
    @PreAuthorize("@projectSecurity.isProjectMember(#projectId, authentication)")
    public ResponseEntity<List<RemarkDTO>> getProjectRemarks(@PathVariable Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found");
        }

        List<Remark> remarks = remarkRepository.findByProjectIdOrderByCreationDateDesc(projectId);
        return ResponseEntity.ok(remarks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList()));
    }

    @PostMapping
    @PreAuthorize("hasRole('EVALUATEUR') and @projectSecurity.isProjectReviewer(#projectId, authentication)")
    public ResponseEntity<RemarkDTO> addRemark(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        Remark remark = remarkService.addRemark(projectId, content, authentication.getName());
        return ResponseEntity.ok(convertToDto(remark));
    }

    private RemarkDTO convertToDto(Remark remark) {
        RemarkDTO dto = new RemarkDTO();
        dto.setId(remark.getId());
        dto.setContent(remark.getContent());
        dto.setCreationDate(remark.getCreationDate());

        // Ajoutez ces champs pour le statut admin
        dto.setAdminStatus(remark.getAdminStatus() != null ? remark.getAdminStatus().toString() : null);
        dto.setValidationDate(remark.getValidationDate());

        if (remark.getReviewer() != null) {
            dto.setReviewerId(remark.getReviewer().getId());
            dto.setReviewerName(remark.getReviewer().getPrenom() + " " + remark.getReviewer().getNom());
        }

        return dto;
    }
}