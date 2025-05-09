package com.example.cerbo.repository;

import com.example.cerbo.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:searchTerm IS NULL OR :searchTerm = '' OR " +
            "LOWER(a.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.actionType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.entityType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.details) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:actionType IS NULL OR :actionType = '' OR a.actionType = :actionType) AND " +
            "(CAST(:date AS date) IS NULL OR CAST(a.timestamp AS date) = CAST(:date AS date))")
    Page<AuditLog> searchAuditLogs(
            @Param("searchTerm") String searchTerm,
            @Param("actionType") String actionType,
            @Param("date") LocalDate date,
            Pageable pageable);
}