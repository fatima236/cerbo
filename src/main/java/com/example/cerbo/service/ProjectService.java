package com.example.cerbo.service;

import com.example.cerbo.dto.ProjectSubmissionDTO;
import com.example.cerbo.entity.*;
import com.example.cerbo.entity.enums.DocumentType;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    @Transactional
    public Project submitProject(ProjectSubmissionDTO submissionDTO) {
        // Validation des champs obligatoires
        if (submissionDTO.getPrincipalInvestigatorId() == null) {
            throw new IllegalArgumentException("L'investigateur principal est obligatoire");
        }

        // Construction du projet SANS documents pour l'instant
        Project project = Project.builder()
                .title(submissionDTO.getTitle())
                .studyDuration(submissionDTO.getStudyDuration())
                .targetPopulation(submissionDTO.getTargetPopulation())
                .consentType(submissionDTO.getConsentType())
                .sampling(submissionDTO.getSampling())
                .sampleType(submissionDTO.getSampleType())
                .sampleQuantity(submissionDTO.getSampleQuantity())
                .fundingSource(submissionDTO.getFundingSource())
                .fundingProgram(submissionDTO.getFundingProgram())
                .projectDescription(submissionDTO.getProjectDescription())
                .ethicalConsiderations(submissionDTO.getEthicalConsiderations())
                .status(ProjectStatus.SOUMIS)
                .submissionDate(LocalDateTime.now())
                .documents(new ArrayList<>()) // Empty list for now
                .build();

        // Investigateur principal
        User principal = userRepository.findById(submissionDTO.getPrincipalInvestigatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Investigateur principal non trouvé"));
        project.setPrincipalInvestigator(principal);

        // Co-investigateurs
        Set<User> investigators = new HashSet<>();
        investigators.add(principal);
        if (submissionDTO.getInvestigatorIds() != null) {
            submissionDTO.getInvestigatorIds().forEach(id -> {
                User inv = userRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Investigateur non trouvé"));
                investigators.add(inv);
            });
        }
        project.setInvestigators(investigators);

        // Save the project first to generate its ID
        Project savedProject = projectRepository.save(project);

        // Now create and associate documents with the saved project
        List<Document> documents = new ArrayList<>();
        addDocumentIfPresent(documents, submissionDTO.getInfoSheetFrPath(), DocumentType.INFORMATION_SHEET_FR, savedProject);
        addDocumentIfPresent(documents, submissionDTO.getInfoSheetArPath(), DocumentType.INFORMATION_SHEET_AR, savedProject);
        addDocumentIfPresent(documents, submissionDTO.getConsentFormFrPath(), DocumentType.CONSENT_FORM_FR, savedProject);
        addDocumentIfPresent(documents, submissionDTO.getConsentFormArPath(), DocumentType.CONSENT_FORM_AR, savedProject);
        addDocumentIfPresent(documents, submissionDTO.getCommitmentCertificatePath(), DocumentType.COMMITMENT_CERTIFICATE, savedProject);
        addDocumentIfPresent(documents, submissionDTO.getCvPath(), DocumentType.INVESTIGATOR_CV, savedProject);
        addDocumentIfPresent(documents, submissionDTO.getProjectDescriptionFilePath(), DocumentType.PROJECT_DESCRIPTION, savedProject);
        addDocumentIfPresent(documents, submissionDTO.getEthicalConsiderationsFilePath(), DocumentType.ETHICAL_CONSIDERATIONS, savedProject);

        // Autres documents
        if (submissionDTO.getOtherDocumentsPaths() != null) {
            submissionDTO.getOtherDocumentsPaths().forEach(path ->
                    addDocumentIfPresent(documents, path, DocumentType.OTHER, savedProject));
        }

        // Set the documents and save again
        savedProject.setDocuments(documents);
        return projectRepository.save(savedProject);
    }

    private void addDocumentIfPresent(List<Document> documents, String filePath, DocumentType type, Project project) {
        if (filePath != null && !filePath.isEmpty()) {
            Document doc = Document.builder()
                    .type(type)
                    .name(filePath.substring(filePath.lastIndexOf('/') + 1))
                    .path(filePath)
                    .project(project)  // Set the project reference
                    .creationDate(LocalDateTime.now())
                    .build();
            documents.add(doc);
        }
    }

    // Les autres méthodes restent inchangées...
    public Page<Project> findFilteredProjects(ProjectStatus status, String search, Pageable pageable) {
        Specification<Project> spec = Specification.where(null);

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (search != null && !search.isEmpty()) {
            String likePattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), likePattern),
                    cb.like(cb.lower(root.get("reference")), likePattern),
                    cb.like(cb.lower(root.get("principalInvestigator").get("email")), likePattern)
            ));
        }

        return projectRepository.findAll(spec, pageable);
    }

    @Transactional
    public Project assignReviewers(Long projectId, Set<Long> reviewerIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Set<User> reviewers = userRepository.findAllById(reviewerIds).stream()
                .filter(user -> user.getRoles().contains("EVALUATEUR"))
                .collect(Collectors.toSet());

        if (reviewers.size() != reviewerIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some users are not evaluators");
        }

        project.setReviewers(reviewers);
        project.setStatus(ProjectStatus.EN_COURS);
        project.setReviewDate(LocalDateTime.now());

        return projectRepository.save(project);
    }

    public List<Project> findUserProjects(Long userId, ProjectStatus status) {
        Specification<Project> spec = (root, query, cb) -> cb.or(
                cb.equal(root.get("principalInvestigator").get("id"), userId),
                cb.isMember(userId, root.get("investigators").get("id"))
        );

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return projectRepository.findAll(spec);
    }
    // Ajoutez cette méthode dans votre ProjectService


    // ... autres dépendances

    public List<Document> getProjectDocuments(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return project.getDocuments();
    }
    // Add these methods to your ProjectService class

    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        return projectRepository.findAllWithInvestigatorsAndReviewers();
    }
    @Transactional
    public Project getProjectById(Long id) {
        return projectRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }
    @Transactional
    public Project updateProjectStatus(Long id, String status, String comment) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        try {
            ProjectStatus newStatus = ProjectStatus.valueOf(status);
            project.setStatus(newStatus);



            // You might want to add additional logic here based on status changes
            // For example, send notifications when status changes

            return projectRepository.save(project);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value: " + status);
        }
    }

    @Transactional
    public Project assignEvaluator(Long projectId, Long evaluatorId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        User evaluator = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + evaluatorId));

        // Check if the user is actually an evaluator
        if (!evaluator.getRoles().contains("EVALUATEUR")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User with id " + evaluatorId + " is not an evaluator");
        }

        Set<User> reviewers = project.getReviewers();
        if (reviewers == null) {
            reviewers = new HashSet<>();
        }
        reviewers.add(evaluator);
        project.setReviewers(reviewers);

        // You might want to update the status or send notifications here
        project.setStatus(ProjectStatus.EN_COURS);

        return projectRepository.save(project);
    }
}