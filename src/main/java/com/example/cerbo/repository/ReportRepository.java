package com.example.cerbo.repository;

import com.example.cerbo.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.cerbo.entity.enums.ReportStatus;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report,Long> {
    List<Report> findByProjectId(Long projectId);
    Optional<Report> findTopByProjectIdAndStatusOrderByCreatedAtDesc(
            Long projectId,
            ReportStatus status
    );
    List<Report> findByProjectIdOrderByCreationDateDesc(Long projectId);
}
