package com.example.cerbo.controller;

import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.nimbusds.jose.util.Resource;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
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
import java.nio.file.Files;
import java.nio.file.Paths;
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
    public ResponseEntity<?> getAllProjects(        @RequestParam(required = false) String search,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    Authentication authentication) {
        try {
            log.info("Attempting to get all projects");
            log.info("Authenticated user: {}", authentication.getName());
            log.info("User authorities: {}", authentication.getAuthorities());
            log.info("Search params - search: {}, status: {}", search, status);

            // Convertir le statut string en enum si fourni
            ProjectStatus statusEnum = null;
            if (status != null && !status.isEmpty()) {
                try {
                    statusEnum = ProjectStatus.valueOf(status);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("Statut invalide: " + status);
                }
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("submissionDate").descending());

            Page<Project> projectsPage = projectService.findFilteredProjects(statusEnum, search, pageable);
            List<Project> projects = projectsPage.getContent();


            // Ajoutez ce logging pour voir ce qui est récupéré depuis la base
            //List<Project> projects = projectService.getAllProjects();
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
    public ResponseEntity<Map<String, Object>> getProjectById(@PathVariable Long id) {
        try {
            Project project = projectService.getProjectById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("id", project.getId());
            response.put("title", project.getTitle());
            response.put("status", project.getStatus().name());
            response.put("submissionDate", project.getSubmissionDate());
            response.put("reference", project.getReference());
            response.put("studyDuration", project.getStudyDuration());
            response.put("targetPopulation", project.getTargetPopulation());
            response.put("consentType", project.getConsentType());
            response.put("sampling", project.getSampling());
            response.put("sampleType", project.getSampleType());
            response.put("sampleQuantity", project.getSampleQuantity());
            response.put("fundingSource", project.getFundingSource());
            response.put("fundingProgram", project.getFundingProgram());

            // Investigateur principal
            if (project.getPrincipalInvestigator() != null) {
                Map<String, Object> investigator = new HashMap<>();
                investigator.put("id", project.getPrincipalInvestigator().getId());
                investigator.put("email", project.getPrincipalInvestigator().getEmail());
                investigator.put("firstName", project.getPrincipalInvestigator().getPrenom());
                investigator.put("lastName", project.getPrincipalInvestigator().getNom());
                response.put("principalInvestigator", investigator);
            }

            // Documents
            List<Map<String, Object>> documents = project.getDocuments().stream()
                    .map(doc -> {
                        Map<String, Object> docMap = new HashMap<>();
                        docMap.put("name", doc.getName());
                        docMap.put("type", doc.getType().name());
                        docMap.put("path", doc.getPath());
                        docMap.put("size", doc.getSize()); // Assurez-vous d'avoir cette propriété
                        return docMap;
                    })
                    .collect(Collectors.toList());
            response.put("documents", documents);

            // Évaluateurs
            List<Map<String, Object>> reviewers = project.getReviewers().stream()
                    .map(reviewer -> {
                        Map<String, Object> reviewerMap = new HashMap<>();
                        reviewerMap.put("id", reviewer.getId());
                        reviewerMap.put("email", reviewer.getEmail());
                        reviewerMap.put("firstName", reviewer.getPrenom());
                        reviewerMap.put("lastName", reviewer.getNom());
                        return reviewerMap;
                    })
                    .collect(Collectors.toList());
            response.put("reviewers", reviewers);

            return ResponseEntity.ok(response);
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
// Dans ProjectController.java

    @GetMapping("/{projectId}/documents/{documentName}/content")
    public ResponseEntity<byte[]> getDocumentContent(
            @PathVariable Long projectId,
            @PathVariable String documentName) throws IOException {

        // Vérifiez que le document appartient bien au projet
        Project project = projectService.getProjectById(projectId);
        boolean documentExists = project.getDocuments().stream()
                .anyMatch(doc -> doc.getName().equals(documentName));

        if (!documentExists) {
            throw new ResourceNotFoundException("Document not found");
        }

        // Récupérez le contenu du fichier
        byte[] content = fileStorageService.loadFileAsBytes(documentName);

        // Déterminez le type MIME
        String contentType = Files.probeContentType(Paths.get(documentName));
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + documentName + "\"")
                .body(content);
    }
}