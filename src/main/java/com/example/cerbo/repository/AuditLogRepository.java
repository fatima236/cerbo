package com.example.cerbo.repository;

import com.example.cerbo.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Pageable;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

}