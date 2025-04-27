package com.example.cerbo.service;

import com.example.cerbo.dto.ProjectSubmissionDTO;
import lombok.SneakyThrows;
import com.example.cerbo.entity.*;

import java.util.HashSet;
import org.springframework.data.domain.Pageable;
import java.util.Set;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import com.example.cerbo.entity.enums.DocumentType;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;


    @Transactional
    @SneakyThrows
    public Project submitProject(ProjectSubmissionDTO submissionDTO) {
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
        Project savedProject = projectRepository.save(project);



        // 2. Définir l'investigateur principal
        User principalInvestigator = userRepository.findById(submissionDTO.getPrincipalInvestigatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Investigateur principal non trouvé"));
        project.setPrincipalInvestigator(principalInvestigator);

        // 3. Ajouter les co-investigateurs
        Set<User> investigators = new HashSet<>();
        investigators.add(principalInvestigator); // L'investigateur principal est inclus

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

        // Documents obligatoires
        addDocumentIfPresent(documents, submissionDTO.getInfoSheetFr(), DocumentType.FICHE_INFORMATION_FR, project);
        addDocumentIfPresent(documents, submissionDTO.getInfoSheetAr(), DocumentType.FICHE_INFORMATION_AR, project);
        addDocumentIfPresent(documents, submissionDTO.getConsentFormFr(), DocumentType.FICHE_CONSENTEMENT_FR, project);
        addDocumentIfPresent(documents, submissionDTO.getConsentFormAr(), DocumentType.FICHE_CONSENTEMENT_AR, project);
        addDocumentIfPresent(documents, submissionDTO.getCommitmentCertificate(), DocumentType.ATTESTATION_ENGAGEMENT, project);
        addDocumentIfPresent(documents, submissionDTO.getCv(), DocumentType.CV_INVESTIGATEUR, project);

        // Documents optionnels
        if (submissionDTO.getProjectDescriptionFile() != null) {
            addDocumentIfPresent(documents, submissionDTO.getProjectDescriptionFile(), DocumentType.DESCRIPTIF_PROJET, project);
        }

        if (submissionDTO.getEthicalConsiderationsFile() != null) {
            addDocumentIfPresent(documents, submissionDTO.getEthicalConsiderationsFile(), DocumentType.CONSIDERATION_ETHIQUE, project);
        }

        if (submissionDTO.getOtherDocuments() != null) {
            for (MultipartFile file : submissionDTO.getOtherDocuments()) {
                addDocumentIfPresent(documents, file, DocumentType.AUTRE, project);
            }
        }

        // Validation des champs obligatoires
        if (submissionDTO.getPrincipalInvestigatorId() == null ||
                submissionDTO.getTitle() == null ||
                submissionDTO.getConsentType() == null) {
            throw new IllegalArgumentException("Champs obligatoires manquants");
        }

        project.setDocuments(documents);

        notificationService.notifyProjectSubmitted(savedProject);


        // 5. Sauvegarder le projet
        return projectRepository.save(project);
    }

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
    @SneakyThrows
    private void addDocumentIfPresent(List<Document> documents, MultipartFile file, DocumentType type, Project project) {
        if (file != null && !file.isEmpty()) {
            String fileName = fileStorageService.storeFile(file);

            Document document = new Document();
            document.setType(type);
            document.setName(file.getOriginalFilename());
            document.setPath(fileName);
            document.setContentType(file.getContentType());
            document.setSize(file.getSize());
            document.setProject(project);

            documents.add(document);
        }
    }
// 1. Ajoutez cette injection de dépendance en haut de la classe
    // 2. Modifiez la méthode assignReviewers
    @Transactional
    public Project assignReviewers(Long projectId, Set<Long> reviewerIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Set<User> reviewers = userRepository.findAllById(reviewerIds).stream()
                .filter(user -> user.getRoles().contains("EVALUATEUR"))
                .collect(Collectors.toSet());

        if (reviewers.size() != reviewerIds.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Some users are not evaluators");
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

    public void notifyProjectSubmitted(Project project) {
        // Notification pour l'admin
        String message = "Nouveau projet soumis: " + project.getTitle();
        notificationService.createNotification("admin@email.com", message);

        // Notification pour l'investigateur
        String userMessage = "Votre projet a été soumis avec succès";
        notificationService.createNotification(
                project.getPrincipalInvestigator().getEmail(),
                userMessage
        );
    }

}