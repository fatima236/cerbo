package com.example.cerbo.controller;

import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.service.NotificationService;
import org.springframework.core.io.Resource;
import jakarta.transaction.Transactional;
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
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.entity.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
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
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;


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
           // Project projet = projectRepository.findByIdWithReviewers(id)
                  //  .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            //Project project = projectService.getProjectById(id);

            Project project = projectRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

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

    // Récupérer tous les évaluateurs
    @GetMapping("/evaluators")
    public ResponseEntity<List<Map<String, Object>>> getAllEvaluators() {
        try {
            List<User> evaluators = userRepository.findByRolesContaining("EVALUATEUR");

            // Assurez-vous que les relations nécessaires sont chargées
            evaluators.forEach(evaluator -> {
                evaluator.setPassword(null); // Pour des raisons de sécurité
            });

            List<Map<String, Object>> response = evaluators.stream()
                    .map(evaluator -> {
                        Map<String, Object> evaluatorMap = new HashMap<>();
                        evaluatorMap.put("id", evaluator.getId());
                        evaluatorMap.put("email", evaluator.getEmail());
                        evaluatorMap.put("firstName", evaluator.getPrenom());
                        evaluatorMap.put("lastName", evaluator.getNom());
                        return evaluatorMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting evaluators", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Assigner plusieurs évaluateurs à un projet
    @PostMapping("/{projectId}/assign-evaluators")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> assignEvaluatorsToProject(
            @PathVariable Long projectId,
            @RequestBody List<Long> evaluatorIds) {

        try {
            // Charge le projet avec ses reviewers
            Project project = projectRepository.findByIdWithReviewers(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

            // Vérifie les évaluateurs existants
            List<User> evaluators = userRepository.findAllById(evaluatorIds);
            if (evaluators.isEmpty()) {
                return ResponseEntity.badRequest().body("Aucun évaluateur valide fourni");
            }

            // Vérifie les rôles
            evaluators.forEach(evaluator -> {
                if (!evaluator.getRoles().contains("EVALUATEUR")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "L'utilisateur " + evaluator.getEmail() + " n'est pas un évaluateur");
                }
            });

            // Filtre les nouveaux évaluateurs
            Set<Long> existingIds = project.getReviewers().stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());

            List<User> newEvaluators = evaluators.stream()
                    .filter(e -> !existingIds.contains(e.getId()))
                    .collect(Collectors.toList());

            if (newEvaluators.isEmpty()) {
                return ResponseEntity.ok().body("Aucun nouvel évaluateur à ajouter");
            }

            // Ajout des évaluateurs
            project.getReviewers().addAll(newEvaluators);
            project.setStatus(ProjectStatus.EN_COURS);
            project.setReviewDate(LocalDateTime.now());

            projectRepository.saveAndFlush(project); // Force l'écriture en base

            // Notifications
            newEvaluators.forEach(evaluator -> {
                notificationService.createNotification(
                        evaluator.getEmail(),
                        "Assignation au projet: " + project.getTitle()
                );
            });

            return ResponseEntity.ok(Map.of(
                    "message", "Évaluateurs assignés avec succès",
                    "added", newEvaluators.stream()
                            .map(e -> Map.of(
                                    "id", e.getId(),
                                    "name", e.getPrenom() + " " + e.getNom()
                            ))
                            .collect(Collectors.toList())
            ));

        } catch (Exception e) {
            log.error("Erreur d'assignation", e);
            return ResponseEntity.internalServerError()
                    .body("Erreur technique: " + e.getMessage());
        }
    }

    @DeleteMapping("/{projectId}/evaluators/{evaluatorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeEvaluatorFromProject(
            @PathVariable Long projectId,
            @PathVariable Long evaluatorId) {

        try {
            Project project = projectRepository.findByIdWithReviewers(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

            User evaluator = userRepository.findById(evaluatorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Evaluator not found"));

            // Vérifier si l'évaluateur est bien dans la liste
            boolean removed = project.getReviewers().removeIf(u -> u.getId().equals(evaluatorId));

            if (!removed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cet évaluateur n'est pas assigné à ce projet");
            }
            projectRepository.save(project);

            // Envoyer une notification
            notificationService.createNotification(
                    evaluator.getEmail(),
                    "Vous avez été retiré du projet: " + project.getTitle()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Évaluateur retiré avec succès",
                    "projectId", projectId,
                    "removedEvaluatorId", evaluatorId
            ));
        } catch (Exception e) {
            log.error("Error removing evaluator", e);
            return ResponseEntity.internalServerError()
                    .body("Erreur lors du retrait de l'évaluateur: " + e.getMessage());
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

    @GetMapping("/{projectId}/documents/{documentName}/content")
    public ResponseEntity<byte[]> viewDocument(
            @PathVariable Long projectId,
            @PathVariable String documentName,
            HttpServletRequest request) throws IOException {

        Project project = projectService.getProjectById(projectId);
        boolean documentExists = project.getDocuments().stream()
                .anyMatch(doc -> doc.getName().equals(documentName));

        if (!documentExists) {
            throw new ResourceNotFoundException("Document not found");
        }

        byte[] fileContent = fileStorageService.loadFileAsBytes(documentName);
        // Chargement du fichier
        Resource resource = fileStorageService.loadFileAsResource(documentName);
        String contentType = determineContentType(documentName);

        // Détermination du type de contenu
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }

        String headerValue = "inline; filename=\"" + resource.getFilename() + "\"";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.inline().filename(documentName).build());

        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }

    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif": return "image/gif";
            case "txt": return "text/plain";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default:
                return "application/octet-stream";
        }
    }

    @GetMapping("/{projectId}/documents/{documentName}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long projectId,
            @PathVariable String documentName) throws IOException {

        Project project = projectService.getProjectById(projectId);
        boolean documentExists = project.getDocuments().stream()
                .anyMatch(doc -> doc.getName().equals(documentName));

        if (!documentExists) {
            throw new ResourceNotFoundException("Document not found");
        }

        byte[] fileContent = fileStorageService.loadFileAsBytes(documentName);
        String contentType = determineContentType(documentName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(documentName).build());

        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }

    private String determineContentType(Resource resource) {
        try {
            return Files.probeContentType(resource.getFile().toPath());
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    @GetMapping("/investigator/{userId}")
    @PreAuthorize("hasRole('INVESTIGATEUR')")
    public ResponseEntity<?> getProjectsByInvestigator(@PathVariable Long userId) {
        try {
            log.info("Fetching projects for investigator ID: {}", userId);

            // Vérification d'authentification
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User currentUser = userRepository.findByEmail(auth.getName());
            if (currentUser == null || !currentUser.getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Utilisez la méthode existante du repository
            List<Project> projects = projectRepository.findByPrincipalInvestigatorId(userId);

            // Simplifiez la réponse
            List<Map<String, Object>> response = projects.stream()
                    .map(p -> {
                        Map<String, Object> projectMap = new HashMap<>();
                        projectMap.put("id", p.getId());
                        projectMap.put("title", p.getTitle());
                        projectMap.put("status", p.getStatus().toString());
                        projectMap.put("submissionDate", p.getSubmissionDate());
                        projectMap.put("reference", p.getReference());

                        projectMap.put("studyDuration", p.getStudyDuration());
                        projectMap.put("targetPopulation", p.getTargetPopulation());
                        projectMap.put("consentType", p.getConsentType());
                        projectMap.put("fundingSource", p.getFundingSource());
                        projectMap.put("fundingProgram", p.getFundingProgram());
                        projectMap.put("sampling", p.getSampling());
                        projectMap.put("sampleType", p.getSampleType());
                        projectMap.put("sampleQuantity", p.getSampleQuantity());

                        // Documents de base
                        projectMap.put("documents", p.getDocuments().stream()
                                .map(d -> Map.of(
                                        "name", d.getName(),
                                        "path", d.getPath()
                                ))
                                .collect(Collectors.toList()));

                        return projectMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting investigator projects: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.toString(), // Utilisez toString() pour plus de détails
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }
    private String formatFileSize(long size) {
        if (size < 1024) return size + " bytes";
        else if (size < 1024 * 1024) return (size / 1024) + " KB";
        else return (size / (1024 * 1024)) + " MB";
    }

    // Récupérer les documents d'un projet spécifique pour l'évaluateur
    @GetMapping("/{projectId}/documents")
    @PreAuthorize("hasRole('EVALUATEUR')")
    public ResponseEntity<?> getProjectDocuments(
            @PathVariable Long projectId,
            Authentication authentication) {

        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User evaluator = userRepository.findByEmail(userDetails.getUsername());

            Project project = projectRepository.findByIdAndReviewerIdWithDocuments(projectId, evaluator.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé ou non assigné à cet évaluateur"));

            List<Map<String, Object>> documents = project.getDocuments().stream()
                    .map(doc -> {
                        Map<String, Object> docMap = new HashMap<>();
                        docMap.put("id", doc.getId());
                        docMap.put("name", doc.getName());
                        docMap.put("type", doc.getType().name());
                        docMap.put("path", doc.getPath());
                        docMap.put("size", doc.getSize());
                        docMap.put("remark", doc.getRemark());
                        docMap.put("submitted", doc.isSubmitted());
                        docMap.put("validated", doc.isValidated());
                        docMap.put("validationDate", doc.getValidationDate());
                        return docMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error getting project documents", e);
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de la récupération des documents: " + e.getMessage());
        }
    }
    @GetMapping("/assigned-to-me")
    @PreAuthorize("hasRole('EVALUATEUR')")
    public ResponseEntity<?> getAssignedProjectsWithDocuments(Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User currentUser = userRepository.findByEmail(userDetails.getUsername());

            // Récupération des projets avec leurs documents en une seule requête
            List<Project> projects = projectRepository.findByReviewerIdWithDocuments(currentUser.getId());

            // Formatage de la réponse
            List<Map<String, Object>> response = projects.stream().map(project -> {
                Map<String, Object> projectMap = new HashMap<>();
                projectMap.put("id", project.getId());
                projectMap.put("title", project.getTitle());
                projectMap.put("reference", project.getReference());
                projectMap.put("description", project.getProjectDescription());
                projectMap.put("dateAttribution", project.getReviewDate());

                // Documents
                List<Map<String, Object>> documents = project.getDocuments().stream()
                        .map(doc -> {
                            Map<String, Object> docMap = new HashMap<>();
                            docMap.put("id", doc.getId());
                            docMap.put("name", doc.getName());
                            docMap.put("size", doc.getSize());
                            docMap.put("remark", doc.getRemark());
                            docMap.put("submitted", doc.isSubmitted());
                            docMap.put("validated", doc.isValidated());
                            return docMap;
                        })
                        .collect(Collectors.toList());

                projectMap.put("fichiers", documents);
                return projectMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in getAssignedProjectsWithDocuments", e);
            return ResponseEntity.internalServerError().body("Error retrieving projects");
        }
    }
    // Mettre à jour les remarques sur un document
    @PutMapping("/{projectId}/documents/{documentId}")
    @PreAuthorize("hasRole('EVALUATEUR')")
    public ResponseEntity<?> updateDocumentEvaluation(
            @PathVariable Long projectId,
            @PathVariable Long documentId,
            @RequestBody Map<String, Object> updates,
            Authentication authentication) {

        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User evaluator = userRepository.findByEmail(userDetails.getUsername());

            // Vérifier que le projet est bien assigné à l'évaluateur
            Project project = projectRepository.findByIdAndReviewerIdWithDocuments(projectId, evaluator.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé ou non assigné à cet évaluateur"));

            // Vérifier si le délai d'évaluation est dépassé
            if (project.getReviewDate() != null &&
                    LocalDateTime.now().isAfter(project.getReviewDate().plusDays(60))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Le délai d'évaluation de ce projet est dépassé");
            }

            // Trouver le document
            Document document = project.getDocuments().stream()
                    .filter(d -> d.getId().equals(documentId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé"));

            // Mettre à jour les champs
            if (updates.containsKey("remark")) {
                document.setRemark((String) updates.get("remark"));
            }
            if (updates.containsKey("submitted")) {
                document.setSubmitted((Boolean) updates.get("submitted"));
            }
            if (updates.containsKey("validated")) {
                boolean validated = (Boolean) updates.get("validated");
                document.setValidated(validated);
                if (validated) {
                    document.setValidationDate(LocalDateTime.now());
                }
            }
            document.setModificationDate(LocalDateTime.now());

            projectRepository.save(project);

            return ResponseEntity.ok(Map.of(
                    "message", "Évaluation du document mise à jour avec succès",
                    "documentId", documentId
            ));
        } catch (Exception e) {
            log.error("Error updating document evaluation", e);
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de la mise à jour de l'évaluation: " + e.getMessage());
        }
    }

    // Marquer l'évaluation comme complète
    @PostMapping("/{projectId}/complete-evaluation")
    @PreAuthorize("hasRole('EVALUATEUR')")
    public ResponseEntity<?> completeEvaluation(
            @PathVariable Long projectId,
            Authentication authentication) {

        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User evaluator = userRepository.findByEmail(userDetails.getUsername());

            Project project = projectRepository.findByIdAndReviewerIdWithDocuments(projectId, evaluator.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé ou non assigné à cet évaluateur"));

            // Vérifier que tous les documents ont été traités
            boolean allDocumentsProcessed = project.getDocuments().stream()
                    .allMatch(doc -> doc.isValidated() || doc.isSubmitted());

            if (!allDocumentsProcessed) {
                return ResponseEntity.badRequest()
                        .body("Tous les documents doivent être validés ou avoir des remarques avant de compléter l'évaluation");
            }

            // Envoyer une notification
            notificationService.createNotification(
                    "admin@example.com", // À remplacer par l'email de l'admin
                    "Évaluation complétée pour le projet: " + project.getTitle() + " par " + evaluator.getNom()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Évaluation complétée avec succès",
                    "projectId", projectId,
                    "evaluatorId", evaluator.getId()
            ));
        } catch (Exception e) {
            log.error("Error completing evaluation", e);
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de la complétion de l'évaluation: " + e.getMessage());
        }
    }

}








