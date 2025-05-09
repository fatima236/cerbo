package com.example.cerbo.controller;

import com.example.cerbo.annotation.Loggable;
import com.example.cerbo.entity.AuditLog;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest request;

    @AfterReturning(
            pointcut = "@annotation(loggable)",
            returning = "result"
    )
    public void logAction(JoinPoint joinPoint, Loggable loggable, Object result) {
        try {
            AuditLog log = new AuditLog();
            log.setTimestamp(LocalDateTime.now());
            log.setActionType(loggable.actionType());
            log.setEntityType(loggable.entityType());
            log.setIpAddress(request.getRemoteAddr());

            // User info
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                log.setUsername(authentication.getName());
            }

            // Entity ID extraction
            if (result != null) {
                if (result instanceof User) {
                    log.setEntityId(((User) result).getId());
                } else if (result instanceof BaseEntity) {
                    log.setEntityId(((BaseEntity) result).getId());
                }
            }

            // Additional details from method arguments
            String details = Arrays.stream(joinPoint.getArgs())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            log.setDetails("Arguments: " + details);

            auditLogRepository.save(log);
        } catch (Exception e) {
            // Log the error but don't interrupt the flow
            System.err.println("Failed to save audit log: " + e.getMessage());
        }
    }

    @AfterThrowing(pointcut = "@annotation(loggable)", throwing = "ex")
    public void logException(JoinPoint joinPoint, Loggable loggable, Exception ex) {
        AuditLog log = new AuditLog();
        log.setTimestamp(LocalDateTime.now());
        log.setActionType(loggable.actionType() + "_FAILED");
        log.setEntityType(loggable.entityType());
        log.setIpAddress(request.getRemoteAddr());
        log.setDetails("Error: " + ex.getMessage());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            log.setUsername(authentication.getName());
        }

        auditLogRepository.save(log);
    }
}