package com.example.cerbo.controller;

import com.example.cerbo.dto.RemarkDTO;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.RemarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects/{projectId}/remarks")
@RequiredArgsConstructor
public class RemarkController {

    private final RemarkRepository remarkRepository;
    private final ProjectRepository projectRepository;

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

    private RemarkDTO convertToDto(Remark remark) {
        RemarkDTO dto = new RemarkDTO();
        dto.setId(remark.getId());
        dto.setContent(remark.getContent());
        dto.setCreationDate(remark.getCreationDate());

        if (remark.getReviewer() != null) {
            dto.setReviewerId(remark.getReviewer().getId());
            dto.setReviewerName(remark.getReviewer().getPrenom());
            dto.setReviewerName(remark.getReviewer().getNom());
        }

        return dto; // N'oubliez pas ce return statement!
    }
}
