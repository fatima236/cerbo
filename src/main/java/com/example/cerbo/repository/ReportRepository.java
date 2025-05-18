package com.example.cerbo.repository;

import com.example.cerbo.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report,Long> {
    List<Report> findByProjectId(Long projectId);
}
