package com.example.cerbo.controller;

import com.example.cerbo.entity.AuditLog;
import com.example.cerbo.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/adminsys")
@PreAuthorize("hasRole('ADMINSYS')")
public class AdminSysController {

    private final AuditLogRepository auditLogRepository;

    public AdminSysController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/audit-logs")
    public Page<AuditLog> getAllAuditLogs(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return auditLogRepository.searchAuditLogs(search, actionType, date, pageable);
    }
}