package com.example.cerbo.controller;

import com.example.cerbo.annotation.Loggable;
import com.example.cerbo.entity.AuditLog;
import com.example.cerbo.controller.BaseEntity;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private final ApplicationEventPublisher eventPublisher;
    private final HttpServletRequest request;

    @AfterReturning(pointcut = "@annotation(loggable)", returning = "result")
    public void logAction(JoinPoint joinPoint, Loggable loggable, Object result) {
        try {
            String username = getCurrentUsername();
            Long userId = getCurrentUserId();
            Long entityId = extractEntityId(result);

            AuditLogEvent event = new AuditLogEvent(
                    loggable.actionType(),
                    loggable.entityType(),
                    entityId,
                    username,
                    userId,
                    getClientIp(),
                    joinPoint.getSignature().getName(),
                    getMethodArgumentsDetails(joinPoint)
            );

            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            System.err.println("Failed to process audit log: " + e.getMessage());
        }
    }

    @AfterThrowing(pointcut = "@annotation(loggable)", throwing = "ex")
    public void logException(JoinPoint joinPoint, Loggable loggable, Exception ex) {
        AuditLogEvent event = new AuditLogEvent(
                loggable.actionType() + "_FAILED",
                loggable.entityType(),
                null,
                getCurrentUsername(),
                getCurrentUserId(),
                getClientIp(),
                joinPoint.getSignature().getName(),
                "Error: " + ex.getMessage()
        );
        eventPublisher.publishEvent(event);
    }

    // Méthodes utilitaires
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        return null;
    }

    private String getClientIp() {
        return request != null ? request.getRemoteAddr() : "internal";
    }

    private Long extractEntityId(Object result) {
        if (result instanceof Project) {
            return ((Project) result).getId();
        } else if (result instanceof BaseEntity) {
            return ((BaseEntity) result).getId();
        }
        return null;
    }

    private String getMethodArgumentsDetails(JoinPoint joinPoint) {
        return Arrays.stream(joinPoint.getArgs())
                .map(arg -> {
                    if (arg instanceof Collection) {
                        return "collection(size=" + ((Collection<?>) arg).size() + ")";
                    }
                    return arg != null ? arg.toString() : "null";
                })
                .collect(Collectors.joining(", "));
    }

    // Classe d'événement interne
    @RequiredArgsConstructor
    private static class AuditLogEvent {
        private final String actionType;
        private final String entityType;
        private final Long entityId;
        private final String username;
        private final Long userId;
        private final String ipAddress;
        private final String methodName;
        private final String details;
    }

    // Écouteur d'événements
    @Component
    @RequiredArgsConstructor
    private static class AuditLogEventListener {
        private final AuditLogRepository auditLogRepository;

        @EventListener
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void handleAuditLogEvent(AuditLogEvent event) {
            AuditLog log = new AuditLog();
            log.setTimestamp(LocalDateTime.now());
            log.setActionType(event.actionType);
            log.setEntityType(event.entityType);
            log.setEntityId(event.entityId);
            log.setUsername(event.username);
            log.setUserId(event.userId);
            log.setIpAddress(event.ipAddress);
            log.setMethodName(event.methodName);
            log.setDetails(event.details);

            auditLogRepository.save(log);
        }
    }
}