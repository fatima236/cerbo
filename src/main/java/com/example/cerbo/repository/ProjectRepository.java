package com.example.cerbo.repository;

import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    // Méthodes de base avec chargement des relations
    @EntityGraph(attributePaths = {"principalInvestigator", "investigators", "reviewers"})
    List<Project> findByPrincipalInvestigatorIdOrInvestigatorsId(Long principalId, Long investigatorId);

    @EntityGraph(attributePaths = {"principalInvestigator"})
    @Query("SELECT p FROM Project p WHERE p.principalInvestigator.id = :investigatorId ORDER BY p.submissionDate DESC")
    List<Project> findByPrincipalInvestigatorId(@Param("investigatorId") Long investigatorId);

    @EntityGraph(attributePaths = {"principalInvestigator", "documents"})
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.documents WHERE p.principalInvestigator.id = :userId")
    List<Project> findByPrincipalInvestigatorIdWithDocuments(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"principalInvestigator"})
    List<Project> findByStatus(ProjectStatus status);

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.documentReviews r " +
            "LEFT JOIN FETCH r.reviewer " +
            "WHERE p.principalInvestigator.id = :userId")
    List<Project> findByPrincipalInvestigatorIdWithRemarks(@Param("userId") Long userId);

    // Méthodes de recherche avancée
    @EntityGraph(attributePaths = {"principalInvestigator"})
    @Query("SELECT p FROM Project p WHERE " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:search IS NULL OR " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.reference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.projectDescription) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.principalInvestigator.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.principalInvestigator.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.principalInvestigator.prenom) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Project> findFilteredProjects(
            @Param("status") ProjectStatus status,
            @Param("search") String search,
            Pageable pageable);

    // Alternative simplifiée pour certains cas
    @EntityGraph(attributePaths = {"principalInvestigator"})
    Page<Project> findByStatusAndTitleContainingIgnoreCaseOrReferenceContainingIgnoreCase(
            ProjectStatus status,
            String title,
            String reference,
            Pageable pageable);

    // Méthodes pour le chargement complet des relations
    @EntityGraph(attributePaths = {"principalInvestigator", "investigators", "reviewers", "documents"})
    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.investigators " +
            "LEFT JOIN FETCH p.reviewers " +
            "LEFT JOIN FETCH p.documents")
    List<Project> findAllWithInvestigatorsAndReviewers();

    @EntityGraph(attributePaths = {"principalInvestigator", "investigators", "reviewers", "documents"})
    @Query("SELECT p FROM Project p " +
            "LEFT JOIN FETCH p.principalInvestigator " +
            "LEFT JOIN FETCH p.investigators " +
            "LEFT JOIN FETCH p.reviewers " +
            "LEFT JOIN FETCH p.documents " +
            "WHERE p.id = :id")
    Optional<Project> findByIdWithDetails(@Param("id") Long id);

    // Méthode générique pour les projets utilisateur avec filtrage par statut
    @EntityGraph(attributePaths = {"principalInvestigator"})
    @Query("SELECT p FROM Project p WHERE " +
            "(:userId IN (SELECT i.id FROM p.investigators i) OR p.principalInvestigator.id = :userId) AND " +
            "(:status IS NULL OR p.status = :status)")
    List<Project> findUserProjectsWithStatus(
            @Param("userId") Long userId,
            @Param("status") ProjectStatus status);

    @EntityGraph(attributePaths = {"principalInvestigator"})
    Page<Project> findAll(Specification<Project> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"reviewers"})
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.reviewers WHERE p.id = :id")
    Optional<Project> findByIdWithReviewers(@Param("id") Long id);

    @Query("SELECT DISTINCT u FROM User u WHERE 'EVALUATEUR' MEMBER OF u.roles")
    List<User> findAllEvaluators();
    @EntityGraph(attributePaths = {"principalInvestigator", "documents"})
    @Query("SELECT DISTINCT p FROM Project p JOIN p.reviewers r WHERE r.id = :reviewerId")
    List<Project> findByReviewerIdWithDocuments(@Param("reviewerId") Long reviewerId);

    // Récupérer les projets assignés à un évaluateur sans documents
    @EntityGraph(attributePaths = {"principalInvestigator"})
    @Query("SELECT DISTINCT p FROM Project p JOIN p.reviewers r WHERE r.id = :reviewerId")
    List<Project> findByReviewerId(@Param("reviewerId") Long reviewerId);
    
    // Récupérer un projet spécifique avec ses documents pour un évaluateur
    @EntityGraph(attributePaths = {"principalInvestigator", "documents"})
    @Query("SELECT p FROM Project p JOIN p.reviewers r WHERE p.id = :projectId AND r.id = :reviewerId")
    Optional<Project> findByIdAndReviewerIdWithDocuments(
            @Param("projectId") Long projectId,
            @Param("reviewerId") Long reviewerId);
    @Query("SELECT d FROM Document d WHERE d.id = :documentId AND d.project.id = :projectId")
    Optional<Document> findByIdAndProjectId(@Param("documentId") Long documentId, @Param("projectId") Long projectId);

    long countByStatus(ProjectStatus status);

    List<Project> findByResponseDeadlineBeforeAndStatusNot(LocalDateTime date, ProjectStatus status);


}