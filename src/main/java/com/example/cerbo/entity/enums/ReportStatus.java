package com.example.cerbo.entity.enums;


/**
 * Représente les différents états possibles d'un rapport d'évaluation
 */
public enum ReportStatus {

    NON_ENVOYE("non-envoye"),

    SENT("Envoyé"),

    RESPONDED("Répondu"),

    /**
     * Rapport archivé après traitement complet
     */
    ARCHIVED("Archivé"),

    /**
     * Rapport en retard (délai de réponse dépassé)
     */
    OVERDUE("En retard");

    private final String displayName;

    ReportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}