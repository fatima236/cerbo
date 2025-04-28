package com.example.cerbo.service;

import com.example.cerbo.dto.ProjectSubmissionDTO;
import lombok.SneakyThrows;
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
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
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
        if (submissionDTO.getPrincipalInvestigatorId() == null ||
                submissionDTO.getTitle() == null ||
                submissionDTO.getConsentType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Champs obligatoires manquants");
        }

        // 1. Créer le projet de base
        Project project = new Project();
        project.setTitle(submissionDTO.getTitle());
        project.setStudyDuration(submissionDTO.getStudyDuration());
        project.setTargetPopulation(submissionDTO.getTargetPopulation());
        project.setConsentType(submissionDTO.getConsentType());
        project.setSampling(submissionDTO.getSampling());
        project.setSampleType(submissionDTO.getSampleType());
        project.setSampleQuantity(submissionDTO.getSampleQuantity());
        project.setFundingSource(submissionDTO.getFundingSource());
        project.setProjectDescription(submissionDTO.getProjectDescription());
        project.setEthicalConsiderations(submissionDTO.getEthicalConsiderations());
        project.setStatus(ProjectStatus.SOUMIS);
        project.setSubmissionDate(LocalDateTime.now());

        // 2. Définir l'investigateur principal
        User principalInvestigator = userRepository.findById(submissionDTO.getPrincipalInvestigatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Investigateur principal non trouvé"));
        project.setPrincipalInvestigator(principalInvestigator);

        // 3. Ajouter les co-investigateurs
        Set<User> investigators = new HashSet<>();
        investigators.add(principalInvestigator);

        if (submissionDTO.getInvestigatorIds() != null) {
            for (Long investigatorId : submissionDTO.getInvestigatorIds()) {
                User investigator = userRepository.findById(investigatorId)
                        .orElseThrow(() -> new ResourceNotFoundException("Investigateur non trouvé"));
                investigators.add(investigator);
            }
        }
        project.setInvestigators(investigators);

        // 4. Gérer les documents
        List<Document> documents = new ArrayList<>();

        // Documents principaux (chemins des fichiers)
        addDocumentIfPresent(documents, submissionDTO.getInfoSheetFrPath(), DocumentType.FICHE_INFORMATION_FR, project);
        addDocumentIfPresent(documents, submissionDTO.getInfoSheetArPath(), DocumentType.FICHE_INFORMATION_AR, project);
        addDocumentIfPresent(documents, submissionDTO.getConsentFormFrPath(), DocumentType.FICHE_CONSENTEMENT_FR, project);
        addDocumentIfPresent(documents, submissionDTO.getConsentFormArPath(), DocumentType.FICHE_CONSENTEMENT_AR, project);
        addDocumentIfPresent(documents, submissionDTO.getCommitmentCertificatePath(), DocumentType.ATTESTATION_ENGAGEMENT, project);
        addDocumentIfPresent(documents, submissionDTO.getCvPath(), DocumentType.CV_INVESTIGATEUR, project);
        project.setDocuments(documents);

        // 5. Sauvegarder le projet
        Project savedProject = projectRepository.save(project);

        // 6. Envoyer les notifications
        notificationService.notifyProjectSubmitted(savedProject);

        return savedProject;
    }

    private void addDocumentIfPresent(List<Document> documents, String fileName, DocumentType type, Project project) {
        if (fileName != null && !fileName.isEmpty()) {
            Document document = new Document();
            document.setType(type);
            document.setName(fileName); // Utilisez le nom de fichier stocké
            document.setPath(fileName); // Le chemin est le même que le nom dans ce cas
            document.setProject(project);
            documents.add(document);
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
}