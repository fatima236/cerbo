package com.example.cerbo.repository;

import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


import java.util.List;

@Repository

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
    @EntityGraph(attributePaths = {"principalInvestigator", "investigators", "reviewers"})
    List<Project> findByPrincipalInvestigatorIdOrInvestigatorsId(Long principalId, Long investigatorId);

    // Ajout de cette méthode pour les filtres admin
    @EntityGraph(attributePaths = {"principalInvestigator"})
    @Query("SELECT p FROM Project p WHERE " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:search IS NULL OR " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.reference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.principalInvestigator.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.principalInvestigator.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.principalInvestigator.prenom) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Project> findFilteredProjects(
            @Param("status") ProjectStatus status,
            @Param("search") String search,
            Pageable pageable);

    @EntityGraph(attributePaths = {"principalInvestigator"})
    List<Project> findByPrincipalInvestigatorId(Long investigatorId);

    @EntityGraph(attributePaths = {"principalInvestigator"})
    List<Project> findByStatus(ProjectStatus Status);

    @EntityGraph(attributePaths = {"principalInvestigator", "investigators", "reviewers", "documents"})
    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN p.investigators " +
            "LEFT JOIN p.reviewers " +
            "LEFT JOIN p.documents")
    List<Project> findAllWithInvestigatorsAndReviewers();

    @EntityGraph(attributePaths = {"principalInvestigator"})
    Page<Project> findByStatusAndTitleContainingIgnoreCaseOrReferenceContainingIgnoreCase(
            ProjectStatus status,
            String title,
            String reference,
            Pageable pageable);
}
