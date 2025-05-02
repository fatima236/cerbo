package com.example.cerbo.repository;

import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


import java.util.List;

@Repository

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
    List<Project> findByPrincipalInvestigatorIdOrInvestigatorsId(Long principalId, Long investigatorId);

    // Ajoutez aussi cette méthode pour les filtres admin
    @Query("SELECT p FROM Project p WHERE " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.reference) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Project> findFilteredProjects(
            @Param("status") ProjectStatus status,
            @Param("search") String search,
            Pageable pageable);
    List<Project> findByPrincipalInvestigatorId(Long investigatorId);
    List<Project> findByStatus(ProjectStatus Status);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.investigators LEFT JOIN FETCH p.reviewers")
    List<Project> findAllWithInvestigatorsAndReviewers();
}
