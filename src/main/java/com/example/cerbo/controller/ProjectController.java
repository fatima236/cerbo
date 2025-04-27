package com.example.cerbo.controller;

import com.example.cerbo.dto.ProjectDTO;
import com.example.cerbo.dto.ProjectSubmissionDTO;
import com.example.cerbo.entity.*;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.RemarkRepository;
import com.example.cerbo.repository.UserRepository;
import com.example.cerbo.service.FileStorageService;
import com.example.cerbo.service.NotificationService;
import com.example.cerbo.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final RemarkRepository remarkRepository;
    private final ProjectService projectService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;


    // Nouveau endpoint pour la soumission avec fichiers
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('INVESTIGATEUR')")
    public ResponseEntity<ProjectDTO> submitProject(
            @ModelAttribute ProjectSubmissionDTO submissionDTO,
            @AuthenticationPrincipal User principal) {

        // Validation manuelle supplémentaire
        if (submissionDTO.getTitle() == null || submissionDTO.getTitle().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Associer l'utilisateur connecté comme investigateur principal
        submissionDTO.setPrincipalInvestigatorId(principal.getId());

        Project project = projectService.submitProject(submissionDTO);
        ProjectDTO projectDTO = convertToDto(project);

        return new ResponseEntity<>(projectDTO, HttpStatus.CREATED);
    }

    // Endpoint pour télécharger un document
    @GetMapping("/documents/{filename}")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String filename) {
        byte[] fileContent = fileStorageService.loadFileAsBytes(filename);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(fileContent);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        List<ProjectDTO> projectDTOs = projects.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(projectDTOs);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProjectDTO>> getAdminProjects() {
        List<Project> projects = projectRepository.findAll();
        return ResponseEntity.ok(projects.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveProject(
            @PathVariable Long id,
            @RequestBody String comment,
            @AuthenticationPrincipal User admin) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

        project.setStatus(ProjectStatus.APPROUVE);
        project.setDecisionDate(LocalDateTime.now());

        // Ajouter un commentaire
        Remark remark = new Remark();
        remark.setContent("Projet approuvé: " + comment);
        remark.setReviewer(admin);
        remark.setProject(project);
        remarkRepository.save(remark);

        projectRepository.save(project);

        // Notification
        notificationService.notifyProjectStatusChange(project);

        return ResponseEntity.ok(Map.of(
                "message", "Projet approuvé avec succès",
                "project", convertToDto(project)
        ));
    }

    @GetMapping("/admin/filtered")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProjectDTO>> getFilteredProjects(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submissionDate,desc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));
        Page<Project> projects = projectService.findFilteredProjects(status, search, pageable);

        return ResponseEntity.ok(projects.map(this::convertToDto));
    }

    private Sort.Order[] parseSort(String[] sort) {
        return Arrays.stream(sort)
                .map(s -> s.split(","))
                .map(arr -> new Sort.Order(Sort.Direction.fromString(arr[1]), arr[0]))
                .toArray(Sort.Order[]::new);
    }

    @PostMapping("/{id}/reviewers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectDTO> assignReviewers(
            @PathVariable Long id,
            @RequestBody Set<Long> reviewerIds) {

        Project project = projectService.assignReviewers(id, reviewerIds);
        return ResponseEntity.ok(convertToDto(project));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATEUR', 'INVESTIGATEUR')")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return ResponseEntity.ok(convertToDto(project));
    }

    @GetMapping("/investigator/projects")
    @PreAuthorize("hasRole('INVESTIGATEUR')")
    public ResponseEntity<List<ProjectDTO>> getMyProjects(
            @AuthenticationPrincipal User principal) {

        List<Project> projects = projectRepository.findByPrincipalInvestigatorIdOrInvestigatorsId(
                principal.getId(), principal.getId());

        List<ProjectDTO> projectDTOs = projects.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(projectDTOs);
    }



    @PostMapping("/{id}/assign-evaluator")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignEvaluator(
            @PathVariable Long id,
            @RequestBody Map<String, Long> request) {

        Long evaluatorId = request.get("evaluatorId");
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User evaluator = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluator not found"));

        if (!evaluator.getRoles().contains("EVALUATEUR")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Selected user is not an evaluator"
            ));
        }

        project.getReviewers().add(evaluator);
        project.setStatus(ProjectStatus.EN_COURS);
        project.setReviewDate(LocalDateTime.now());

        projectRepository.save(project);

        return ResponseEntity.ok(Map.of(
                "message", "Evaluator assigned successfully",
                "project", convertToDto(project)
        )     );
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User admin) {

        String statusStr = request.get("status");
        String comment = request.get("comment");

        if (statusStr == null || comment == null || comment.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Status and comment are required"
            ));
        }

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        try {
            ProjectStatus status = ProjectStatus.valueOf(statusStr);
            project.setStatus(status);

            if (status == ProjectStatus.APPROUVE || status == ProjectStatus.REJETE) {
                project.setDecisionDate(LocalDateTime.now());
            }

            // Create remark
            Remark remark = new Remark();
            remark.setContent("Status changed to " + status.getDisplayName() + ": " + comment);
            remark.setCreationDate(LocalDateTime.now());
            remark.setProject(project);
            remark.setReviewer(admin);

            remarkRepository.save(remark);
            projectRepository.save(project);

            return ResponseEntity.ok(Map.of(
                    "message", "Project status updated successfully",
                    "project", convertToDto(project)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid status value"
            ));
        }
    }

    // Méthode utilitaire pour convertir Project en DTO
    private ProjectDTO convertToDto(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setReference(project.getReference());
        dto.setTitle(project.getTitle());
        dto.setSubmissionDate(project.getSubmissionDate());
        dto.setReviewDate(project.getReviewDate());
        dto.setDecisionDate(project.getDecisionDate());
        dto.setStatus(project.getStatus());
        dto.setStudyDuration(project.getStudyDuration());
        dto.setTargetPopulation(project.getTargetPopulation());
        dto.setConsentType(project.getConsentType());
        dto.setSampling(project.getSampling());
        dto.setSampleType(project.getSampleType());
        dto.setSampleQuantity(project.getSampleQuantity());
        dto.setFundingSource(project.getFundingSource());
        dto.setProjectDescription(project.getProjectDescription());
        dto.setEthicalConsiderations(project.getEthicalConsiderations());

        if (project.getPrincipalInvestigator() != null) {
            dto.setPrincipalInvestigatorId(project.getPrincipalInvestigator().getId());
        }

        if (project.getReviewers() != null) {
            dto.setReviewerIds(project.getReviewers().stream()
                    .map(User::getId)
                    .collect(Collectors.toList()));
        }


        return dto;
    }
    @GetMapping("/investigator")
    public ResponseEntity<List<ProjectDTO>> getInvestigatorProjects(
            @AuthenticationPrincipal User user) {

        List<Project> projects = projectRepository.findByPrincipalInvestigatorId(user.getId());
        return ResponseEntity.ok(projects.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList()));
    }

}