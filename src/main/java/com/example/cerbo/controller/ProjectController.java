package com.example.cerbo.controller;

import lombok.extern.slf4j.Slf4j;
import com.example.cerbo.dto.ProjectSubmissionDTO;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.UserRepository;
import com.example.cerbo.service.ProjectService;
import com.example.cerbo.service.FileStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.internal.logging.InternalLogger;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Project> submitProject(
            @RequestPart("projectData") String projectDataJson,
            @RequestPart(value = "infoSheetFr", required = false) MultipartFile infoSheetFr,
            @RequestPart(value = "infoSheetAr", required = false) MultipartFile infoSheetAr,
            @RequestPart(value = "consentFormFr", required = false) MultipartFile consentFormFr,
            @RequestPart(value = "consentFormAr", required = false) MultipartFile consentFormAr,
            @RequestPart(value = "commitmentCertificate", required = false) MultipartFile commitmentCertificate,
            @RequestPart(value = "cv", required = false) MultipartFile cv,
            @RequestPart(value = "projectDescriptionFile", required = false) MultipartFile projectDescriptionFile,
            @RequestPart(value = "ethicalConsiderationsFile", required = false) MultipartFile ethicalConsiderationsFile,
            @RequestPart(value = "otherDocuments", required = false) MultipartFile[] otherDocuments,
            HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        try {
            log.info("Received project submission request");
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            User principalInvestigator = userRepository.findByEmail(userDetails.getUsername());

            if (principalInvestigator == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Authenticated user not found");
            }

            ProjectSubmissionDTO submissionDTO = objectMapper.readValue(projectDataJson, ProjectSubmissionDTO.class);
            submissionDTO.setPrincipalInvestigatorId(principalInvestigator.getId());

            submissionDTO.setInfoSheetFrPath(processFile(infoSheetFr));
            submissionDTO.setInfoSheetArPath(processFile(infoSheetAr));
            submissionDTO.setConsentFormFrPath(processFile(consentFormFr));
            submissionDTO.setConsentFormArPath(processFile(consentFormAr));
            submissionDTO.setCommitmentCertificatePath(processFile(commitmentCertificate));
            submissionDTO.setCvPath(processFile(cv));
            submissionDTO.setProjectDescriptionFilePath(processFile(projectDescriptionFile));
            submissionDTO.setEthicalConsiderationsFilePath(processFile(ethicalConsiderationsFile));

            Project project = projectService.submitProject(submissionDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(project);

        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON format");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing files");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error submitting project: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllProjects(Authentication authentication) {
        try {
            log.info("Attempting to get all projects");
            log.info("Authenticated user: {}", authentication.getName());
            log.info("User authorities: {}", authentication.getAuthorities());

            // Ajoutez ce logging pour voir ce qui est récupéré depuis la base
            List<Project> projects = projectService.getAllProjects();
            log.info("Number of projects retrieved from DB: {}", projects.size());
            projects.forEach(p -> log.info("Project ID: {}, Title: {}", p.getId(), p.getTitle()));

            List<Map<String, Object>> response = projects.stream().map(project -> {
                Map<String, Object> projectMap = new HashMap<>();
                projectMap.put("id", project.getId());
                projectMap.put("title", project.getTitle());
                projectMap.put("status", project.getStatus());
                projectMap.put("submissionDate", project.getSubmissionDate());

                if (project.getPrincipalInvestigator() != null) {
                    Map<String, Object> investigator = new HashMap<>();
                    investigator.put("id", project.getPrincipalInvestigator().getId());
                    investigator.put("name", project.getPrincipalInvestigator().getNom() + " " + project.getPrincipalInvestigator().getPrenom());
                    projectMap.put("principalInvestigator", investigator);
                }

                return projectMap;
            }).collect(Collectors.toList());

            log.info("Successfully built response for {} projects", projects.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving projects", e);
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de la récupération des projets: " + e.getMessage());
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id) {
        try {
            Project project = projectService.getProjectById(id);
            return ResponseEntity.ok(project);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Project not found with id: " + id);
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Project> updateProjectStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            String comment = request.get("comment");

            Project updatedProject = projectService.updateProjectStatus(id, status, comment);
            return ResponseEntity.ok(updatedProject);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error updating project status: " + e.getMessage());
        }
    }

    @PostMapping("/{projectId}/assign-evaluator")
    public ResponseEntity<Project> assignEvaluatorToProject(
            @PathVariable Long projectId,
            @RequestBody Map<String, Long> request) {
        try {
            Long evaluatorId = request.get("evaluatorId");
            Project project = projectService.assignEvaluator(projectId, evaluatorId);
            return ResponseEntity.ok(project);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error assigning evaluator: " + e.getMessage());
        }
    }

    private String processFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        return fileStorageService.storeFile(file);
    }
}