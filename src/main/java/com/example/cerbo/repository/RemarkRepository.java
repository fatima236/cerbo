package com.example.cerbo.repository;

import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.enums.RemarkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RemarkRepository extends JpaRepository<Remark, Long> {
    @Query("SELECT r FROM Remark r WHERE r.project.id = :projectId ORDER BY r.creationDate DESC")
    List<Remark> findByProjectIdOrderByCreationDateDesc(Long projectId);

    @Query("SELECT r FROM Remark r WHERE r.project.id = :projectId AND r.response IS NULL")
    List<Remark> findByProjectIdAndResponseIsNull(Long projectId);

    @Query("SELECT COUNT(r) FROM Remark r WHERE r.project.id = :projectId AND r.response IS NULL")
    long countByProjectIdAndResponseIsNull(Long projectId);

    List<Remark> findByProjectIdAndAdminStatus(Long projectId, RemarkStatus adminStatus);

    List<Remark> findByProjectIdAndIncludedInReportTrue(Long projectId);

    long countByProjectIdAndIncludedInReportTrueAndResponseIsNull(Long projectId);

}