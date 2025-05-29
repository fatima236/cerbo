package com.example.cerbo.controller;

import com.example.cerbo.dto.CoInvestigateurDTO;
import com.example.cerbo.entity.*;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.entity.enums.ReportStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.*;
import com.example.cerbo.service.AvisFavorableService;
import com.example.cerbo.service.NotificationService;
import com.example.cerbo.service.documentReview.DocumentReviewService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import com.example.cerbo.dto.ProjectSubmissionDTO;
import com.example.cerbo.service.ProjectService;
import com.example.cerbo.service.FileStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.internal.logging.InternalLogger;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
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
import com.example.cerbo.entity.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final DocumentReviewRepository documentReviewRepository;
    private final ReportRepository reportRepository;
    private final DocumentReviewService documentReviewService;
    private final DocumentRepository documentRepository;


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
            @RequestPart(value = "motivationLetter", required = false) MultipartFile motivationLetter,
            HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        try {
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            User principalInvestigator = userRepository.findByEmail(userDetails.getUsername());

            if (principalInvestigator == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Authenticated user not found");
            }

            // Désérialisation du JSON
            ProjectSubmissionDTO submissionDTO = objectMapper.readValue(projectDataJson, ProjectSubmissionDTO.class);
            submissionDTO.setPrincipalInvestigatorId(principalInvestigator.getId());

            // Validation des co-investigateurs
            if (submissionDTO.getCoInvestigators() != null) {
                for (CoInvestigateurDTO coInv : submissionDTO.getCoInvestigators()) {
                    if (coInv.getEmail().equals(principalInvestigator.getEmail())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "L'investigateur principal ne peut pas être un co-investigateur");
                    }

                    // Validation supplémentaire si nécessaire
                    if (coInv.getName() == null || coInv.getName().trim().isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Le nom du co-investigateur est obligatoire");
                    }
                }
            }

            // Traitement des fichiers
            submissionDTO.setInfoSheetFrPath(processFile(infoSheetFr));
            submissionDTO.setInfoSheetArPath(processFile(infoSheetAr));
            submissionDTO.setConsentFormFrPath(processFile(consentFormFr));
            submissionDTO.setConsentFormArPath(processFile(consentFormAr));
            submissionDTO.setCommitmentCertificatePath(processFile(commitmentCertificate));
            submissionDTO.setCvPath(processFile(cv));
            submissionDTO.setProjectDescriptionFilePath(processFile(projectDescriptionFile));
            submissionDTO.setEthicalConsiderationsFilePath(processFile(ethicalConsiderationsFile));
            submissionDTO.setMotivationLetterPath(processFile(motivationLetter));

            // Traiter les autres documents
            if (otherDocuments != null && otherDocuments.length > 0) {
                List<String> otherDocsPaths = new ArrayList<>();
                for (MultipartFile file : otherDocuments) {
                    otherDocsPaths.add(processFile(file));
                }
                submissionDTO.setOtherDocumentsPaths(otherDocsPaths);
            }

            Project project = projectService.submitProject(submissionDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(project);

        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format JSON invalide");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de traitement des fichiers");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la soumission du projet: " + e.getMessage());
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
                projectMap.put("reference", project.getReference());

                if (project.getPrincipalInvestigator() != null) {
                    Map<String, Object> investigator = new HashMap<>();
                    investigator.put("id", project.getPrincipalInvestigator().getId());
                    investigator.put("name", project.getPrincipalInvestigator().getNom() + " " + project.getPrincipalInvestigator().getPrenom());
                    projectMap.put("principalInvestigator", investigator);
                }
                if (project.getInvestigators() != null) {
                    User principal = project.getPrincipalInvestigator();
                    List<Map<String, String>> coInvestigators = project.getInvestigators().stream()
                            .filter(inv -> principal != null && !inv.getId().equals(principal.getId()))
                            .map(inv -> {
                                Map<String, String> coInv = new HashMap<>();
                                coInv.put("name", inv.getNom());
                                coInv.put("surname", inv.getPrenom());

                                return coInv;
                            })
                            .collect(Collectors.toList());
                    projectMap.put("coInvestigators", coInvestigators);
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
            response.put("reportStatus", project.getLatestReportStatus());
            response.put("projectDescription", project.getProjectDescription());

            response.put("dataDescription", project.getDataDescription());
            response.put("ethicalConsiderations", project.getEthicalConsiderations());
            List<Report> reports = project.getReports();
            if (reports != null && !reports.isEmpty()) {
                Report lastReport = reports.get(reports.size() - 1);
                response.put("creationDateOfReport", lastReport.getCreationDate());
                response.put("reportResponsed", lastReport.getResponsed());
            } else {
                response.put("creationDateOfReport", null); // ou une valeur par défaut
            }



            if (project.getPrincipalInvestigator() != null) {
                Map<String, Object> investigator = new HashMap<>();
                investigator.put("id", project.getPrincipalInvestigator().getId());
                investigator.put("email", project.getPrincipalInvestigator().getEmail());
                investigator.put("firstName", project.getPrincipalInvestigator().getPrenom());
                investigator.put("lastName", project.getPrincipalInvestigator().getNom());
                // Add these new fields:
                investigator.put("affiliation", project.getPrincipalInvestigator().getAffiliation());
                investigator.put("laboratoire", project.getPrincipalInvestigator().getLaboratoire());
                investigator.put("titre", project.getPrincipalInvestigator().getTitre());
                response.put("principalInvestigator", investigator);
            }
            if (project.getInvestigators() != null) {
                User principal = project.getPrincipalInvestigator();
                List<Map<String, String>> coInvestigators = project.getInvestigators().stream()
                        .filter(inv -> principal != null && !inv.getId().equals(principal.getId()))
                        .map(inv -> {
                            Map<String, String> coInv = new HashMap<>();
                            coInv.put("name", inv.getNom());
                            coInv.put("surname", inv.getPrenom());
                            coInv.put("email", inv.getEmail());
                            coInv.put("title", inv.getTitre() != null ? inv.getTitre() : "");
                            coInv.put("affiliation", inv.getAffiliation() != null ? inv.getAffiliation() : "");
                            coInv.put("laboratory", inv.getLaboratoire() != null ? inv.getLaboratoire() : "");
                            return coInv;
                        })
                        .collect(Collectors.toList());
                response.put("coInvestigators", coInvestigators);
            }


            // Documents
            List<Map<String, Object>> documents = project.getDocuments().stream()
                    .distinct()
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

    private final AvisFavorableService avisFavorableService;
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateProjectStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            String comment = request.get("comment");

            Project project = projectService.getProjectById(id);

            if ("Avis favorable".equals(status)) {
                // Retourner les données de prévisualisation sans générer le document
                Map<String, String> previewData = Map.of(
                        "reference", project.getReference(),
                        "intitule", project.getTitle(),
                        "investigateur", "Pr. " + project.getPrincipalInvestigator().getFullName(),
                        "promoteur", "Admin",
                        "date_debut", project.getSubmissionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        "duree_etude", project.getStudyDuration(),
                        "comment", comment,
                        "needsConfirmation", "true"
                );

                return ResponseEntity.ok(previewData);
            } else {
                // Pour les autres statuts, procéder normalement
                Project updatedProject = projectService.updateProjectStatus(id, status, comment);
                return ResponseEntity.ok(updatedProject);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error updating project status: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/confirm-status")
    public ResponseEntity<Project> confirmProjectStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            String comment = request.get("comment");

            Project updatedProject = projectService.updateProjectStatus(id, status, comment);

            if ("Avis favorable".equals(status)) {
                Path avisPath = avisFavorableService.generateAvisFavorable(updatedProject);
                updatedProject.setAvisFavorablePath(avisPath.toString());
                projectRepository.save(updatedProject);
            }

            return ResponseEntity.ok(updatedProject);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error confirming project status: " + e.getMessage());
        }
    }

    @GetMapping("/{projectId}/avis-favorable/preview")
    public ResponseEntity<Map<String, String>> previewAvisFavorable(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Map<String, String> previewData = Map.of(
                "reference", project.getReference(),
                "intitule", project.getTitle(),
                "investigateur", "Pr. " + project.getPrincipalInvestigator().getFullName(),
                "promoteur", "Admin",
                "date_debut", project.getSubmissionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                "duree_etude", project.getStudyDuration()
        );

        return ResponseEntity.ok(previewData);
    }

    @GetMapping("/{projectId}/avis-favorable/download")
    public ResponseEntity<Resource> downloadAvisFavorable(@PathVariable Long projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

            // Si le document n'existe pas, le générer d'abord
            if (project.getAvisFavorablePath() == null || !Files.exists(Paths.get(project.getAvisFavorablePath()))) {
                Path avisPath = avisFavorableService.generateAvisFavorable(project);
                project.setAvisFavorablePath(avisPath.toString());
                projectRepository.save(project);
            }

            Path filePath = Paths.get(project.getAvisFavorablePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File is not readable");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"avis_favorable_" + project.getReference() + ".pdf\""
                    )
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ByteArrayResource(("Error: " + e.getMessage()).getBytes()));
        }
    }
    @PostMapping("/{projectId}/avis-favorable/send")
    public ResponseEntity<?> sendAvisFavorable(@PathVariable Long projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

            // Initialiser opinionSent si null
            if (project.getOpinionSent() == null) {
                project.setOpinionSent(false);
            }

            // Vérifier si l'avis a déjà été envoyé
            if (project.isOpinionSent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "L'avis favorable a déjà été envoyé"
                ));
            }

            // Generate the document if it doesn't exist
            if (project.getAvisFavorablePath() == null) {
                Path avisPath = avisFavorableService.generateAvisFavorable(project);
                project.setAvisFavorablePath(avisPath.toString());
            }

            // Update project status
            project.setStatus(ProjectStatus.AVIS_FAVORABLE);
            project.setOpinionSent(true);
            project.setOpinionSentDate(LocalDateTime.now());
            projectRepository.save(project);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Avis favorable envoyé avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur lors de l'envoi de l'avis favorable: " + e.getMessage()
            ));
        }
    }
    // Récupérer tous les évaluateurs
    @GetMapping("/evaluators")
    public ResponseEntity<?> getAllEvaluators() {
        try {
            log.info("Fetching all evaluators");

            // 1. Récupération des évaluateurs avec le rôle EVALUATEUR
            List<User> evaluators = userRepository.findByRolesContaining("EVALUATEUR");

            if (evaluators == null || evaluators.isEmpty()) {
                log.warn("No evaluators found in database");
                return ResponseEntity.ok(Collections.emptyList());
            }

            // 2. Construction de la réponse
            List<Map<String, Object>> response = evaluators.stream()
                    .map(evaluator -> {
                        // Sécurité: effacer les données sensibles
                        evaluator.setPassword(null);

                        Map<String, Object> evaluatorMap = new HashMap<>();
                        evaluatorMap.put("id", evaluator.getId());
                        evaluatorMap.put("email", evaluator.getEmail());
                        evaluatorMap.put("firstName", evaluator.getPrenom());
                        evaluatorMap.put("lastName", evaluator.getNom());
                        return evaluatorMap;
                    })
                    .collect(Collectors.toList());

            log.info("Successfully fetched {} evaluators", evaluators.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching evaluators: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal Server Error",
                            "message", "Could not fetch evaluators",
                            "timestamp", LocalDateTime.now()
                    ));
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

            if(project.getLatestReport()!=null){
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Le projet a déjà été traité .");
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

            notificationService.sendNotificationByIds(evaluatorIds,
                    "Projet assigné",
                    "Projet assigné : " + project.getTitle(),
                    "/evaluateur/dashboard/"
                    );

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

            notificationService.sendNotification(evaluator,
                    "Retrait du projet",
                    "Vous avez été retiré du projet \"" + project.getTitle() + "\".",
                    "/evaluateur/dashboard/"
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

    @GetMapping("/{projectId}/documents/{documentName}/preview")
    public ResponseEntity<byte[]> previewDocument(
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
        // Important: Utiliser "inline" pour la prévisualisation au lieu de "attachment"
        headers.setContentDisposition(ContentDisposition.inline().filename(documentName).build());

        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
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

            // Récupération des projets avec les co-investigateurs embarqués
            List<Project> projects = projectRepository.findByPrincipalInvestigatorId(userId);

            List<Map<String, Object>> response = projects.stream()
                    .map(p -> {
                        Map<String, Object> projectMap = new HashMap<>();
                        // Informations de base du projet
                        projectMap.put("id", p.getId());
                        projectMap.put("title", p.getTitle());
                        projectMap.put("status", p.getStatus().toString());
                        projectMap.put("submissionDate", p.getSubmissionDate());
                        projectMap.put("reference", p.getReference());

                        // Investigateur principal
                        User principal = p.getPrincipalInvestigator();
                        if (principal != null) {
                            projectMap.put("principalInvestigatorCivilite", principal.getCivilite());
                            projectMap.put("principalInvestigatorName",
                                    (principal.getNom() + " " + principal.getPrenom()).trim());
                            projectMap.put("principalInvestigatorEmail", principal.getEmail());
                            projectMap.put("principalInvestigatorAffiliation", principal.getAffiliation());
                            projectMap.put("principalInvestigatorLaboratory", principal.getLaboratoire());
                            projectMap.put("principalInvestigatorTitre", principal.getTitre());
                        }

                        // Co-investigateurs (nouvelle version avec la liste embarquée)
                        if (p.getCoInvestigators() != null) {
                            List<Map<String, String>> coInvestigators = p.getCoInvestigators().stream()
                                    .map(coInv -> {
                                        Map<String, String> coInvMap = new HashMap<>();
                                        coInvMap.put("name", coInv.getName());
                                        coInvMap.put("surname", coInv.getSurname());
                                        coInvMap.put("email", coInv.getEmail());
                                        coInvMap.put("title", coInv.getTitle() != null ? coInv.getTitle() : "");
                                        coInvMap.put("affiliation", coInv.getAffiliation() != null ? coInv.getAffiliation() : "");
                                        coInvMap.put("laboratory", coInv.getAddress() != null ? coInv.getAddress() : "");
                                        return coInvMap;
                                    })
                                    .collect(Collectors.toList());
                            projectMap.put("coInvestigators", coInvestigators);
                        } else {
                            projectMap.put("coInvestigators", Collections.emptyList());
                        }

                        projectMap.put("studyDuration", p.getStudyDuration());
                        projectMap.put("targetPopulation", p.getTargetPopulation());
                        projectMap.put("consentType", p.getConsentType());

                        if (p.getConsentType() != null &&
                                (p.getConsentType().equalsIgnoreCase("différé") ||
                                        p.getConsentType().equalsIgnoreCase("dérogation"))) {
                            projectMap.put("motivationLetter", p.getMotivationLetterPath());
                        }

                        projectMap.put("sampling", p.getSampling() != null ?
                                (p.getSampling() ? "Oui" : "Non") : "Non spécifié");
                        projectMap.put("sampleType", p.getSampleType());
                        projectMap.put("sampleQuantity", p.getSampleQuantity());
                        projectMap.put("fundingSource", p.getFundingSource());
                        projectMap.put("fundingProgram", p.getFundingProgram());
                        projectMap.put("projectDescription", p.getProjectDescription());
                        projectMap.put("dataDescription", p.getDataDescription());
                        projectMap.put("ethicalConsiderations", p.getEthicalConsiderations());

                        // Gestion des rapports
                        if (p.getLatestReport() != null) {
                            projectMap.put("reportStatus", p.getLatestReport().getStatus());
                            projectMap.put("responsed", p.getLatestReport().getResponsed());
                        } else {
                            projectMap.put("reportStatus", null);
                            projectMap.put("responsed", null);
                        }

                        // Vérification des remarques répondues
                        Boolean allRemarksResponsed = false;
                        if (p.getLatestReport() != null) {
                            allRemarksResponsed = documentReviewService.allReviewsResponsed(p.getLatestReport().getId());
                        }
                        projectMap.put("allRemarksResponsed", allRemarksResponsed);

                        // Documents
                        projectMap.put("documents", p.getDocuments().stream()
                                .map(d -> Map.of(
                                        "id", d.getId(),
                                        "name", d.getName(),
                                        "path", d.getPath(),
                                        "type", d.getType(),
                                        "canReplace", documentReviewRepository.existsByDocument_IdAndReport_Status(
                                                d.getId(), ReportStatus.SENT)
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
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }
  /*  private String formatFileSize(long size) {
        if (size < 1024) return size + " bytes";
        else if (size < 1024 * 1024) return (size / 1024) + " KB";
        else return (size / (1024 * 1024)) + " MB";
    }*/

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
//                        docMap.put("remark", doc.getRemark());
//                        docMap.put("submitted", doc.isSubmitted());
//                        docMap.put("validated", doc.isValidated());
//                        docMap.put("validationDate", doc.getValidationDate());
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
                projectMap.put("reference", project.getReference());

                projectMap.put("studyDuration", project.getStudyDuration());
                projectMap.put("targetPopulation", project.getTargetPopulation());
                projectMap.put("consentType", project.getConsentType());
                projectMap.put("fundingSource", project.getFundingSource());
                projectMap.put("fundingProgram", project.getFundingProgram());
                projectMap.put("sampling", project.getSampling());
                projectMap.put("sampleType", project.getSampleType());
                projectMap.put("sampleQuantity", project.getSampleQuantity());
                projectMap.put("projectDescription", project.getProjectDescription());
                projectMap.put("ethicalConsiderations", project.getEthicalConsiderations());
                projectMap.put("dataDescription", project.getDataDescription());

                Map<String, Object> investigator = new HashMap<>();
                investigator.put("id", project.getPrincipalInvestigator().getId());
                investigator.put("email", project.getPrincipalInvestigator().getEmail());
// Corriger les clés pour correspondre au frontend
                investigator.put("prenom", project.getPrincipalInvestigator().getPrenom());
                investigator.put("nom", project.getPrincipalInvestigator().getNom());
                investigator.put("affiliation", project.getPrincipalInvestigator().getAffiliation());
                investigator.put("laboratoire", project.getPrincipalInvestigator().getLaboratoire());
                projectMap.put("principalInvestigator", investigator);
                // Documents
                List<Map<String, Object>> documents = project.getDocuments().stream()
                        .map(doc -> {
                            Map<String, Object> docMap = new HashMap<>();
                            DocumentReview review = documentReviewRepository.findByDocumentIdAndReviewerId(doc.getId(),currentUser.getId()).orElse(null);
                            docMap.put("id", doc.getId());
                            docMap.put("name", doc.getName());
                            docMap.put("size", doc.getSize());
                            docMap.put("type", doc.getType());
                            if (review != null) {
                                docMap.put("remark", review.getContent());
                                docMap.put("finalSubmission", review.getFinal_submission());
                                docMap.put("validated", review.isValidated());
                                docMap.put("reviewStatus", review.getStatus()); // IMPORTANT
                                docMap.put("reviewRemark", review.getContent());
                                docMap.put("remarqueOriginal", review.getContent());
                                docMap.put("remarque", review.getContent());
                                docMap.put("remarqueEditMode", !review.getFinal_submission());
                            }
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

//            // Mettre à jour les champs
//            if (updates.containsKey("remark")) {
//                document.setRemark((String) updates.get("remark"));
//            }
//            if (updates.containsKey("submitted")) {
//                document.setSubmitted((Boolean) updates.get("submitted"));
//            }
//            if (updates.containsKey("validated")) {
//                boolean validated = (Boolean) updates.get("validated");
//                document.setValidated(validated);
//                if (validated) {
//                    document.setValidationDate(LocalDateTime.now());
//                }
//            }
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

            notificationService.sendNotification(
                    userRepository.findByRolesContaining("ADMIN"),
                    "Évaluation complétée",
                    "Le projet \"" + project.getTitle() + "\" a été évalué par " + evaluator.getNom() + ".",
                    "/admin/projects/"+project.getId()+"/vueProject"
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




    private final String REPORTS_DIRECTORY = "uploads/reports";



    @GetMapping("/{projectId}/full-report")
    public ResponseEntity<Resource> downloadFullReport(@PathVariable Long projectId) {
        try {
            // 1. Trouver le rapport le plus récent pour ce projet
            List<Report> reports = reportRepository.findByProjectIdOrderByCreationDateDesc(projectId);

            if (reports.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Report report = reports.get(0);

            // 2. Construire le chemin complet du fichier
            Path filePath = Paths.get(REPORTS_DIRECTORY, report.getFileName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            // 3. Vérifier que le fichier existe et est accessible
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // 4. Préparer la réponse avec les headers appropriés
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + report.getFileName() + "\""
                    )
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{docId}/documents/replace")
    public ResponseEntity<Document> replaceDoc(@PathVariable("docId") Long docId,
                                               @RequestBody MultipartFile file) {
        Document document = documentRepository.findById(docId).get();
        if (document == null) {
            ResponseEntity.badRequest().body("Le document n'existe pas");
        }
        if(file.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String filename = fileStorageService.updateFile(file,document);
        document.setModificationDate(LocalDateTime.now());
        document.setName(filename);

        return  ResponseEntity.ok(documentRepository.save(document));
    }






}


