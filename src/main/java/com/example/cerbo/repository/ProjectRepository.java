package com.example.cerbo.repository;

import com.example.cerbo.entity.Project;
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

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    // Méthodes de base avec chargement des relations
    @EntityGraph(attributePaths = {"principalInvestigator", "investigators", "reviewers"})
    List<Project> findByPrincipalInvestigatorIdOrInvestigatorsId(Long principalId, Long investigatorId);

    @EntityGraph(attributePaths = {"principalInvestigator"})
    List<Project> findByPrincipalInvestigatorId(Long investigatorId);

    @EntityGraph(attributePaths = {"principalInvestigator"})
    List<Project> findByStatus(ProjectStatus status);

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
}