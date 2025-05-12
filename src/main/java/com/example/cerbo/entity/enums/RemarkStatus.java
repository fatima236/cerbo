package com.example.cerbo.entity.enums;

public enum RemarkStatus {
    PENDING,    // En attente de validation
    DRAFT,
    PENDING_VALIDATION,
    VALIDATED,  // Validée par l'admin
    REJECTED,    // Rejetée par l'admin
    MODIFIED,    // Modifiée par l'admin
    REVIEWED
}