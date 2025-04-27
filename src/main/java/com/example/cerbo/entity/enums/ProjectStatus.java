package com.example.cerbo.entity.enums;

public enum ProjectStatus {
    SOUMIS("Soumis"),
    EN_COURS("En cours d'évaluation"),
    REVISION_MINEURE("Révision mineure requise"),
    REVISION_MAJEURE("Révision majeure requise"),
    AVIS_FAVORABLE("Avis favorable"),
    APPROUVE("Approuvé"),
    REJETE("Rejeté");

    private final String displayName;


    ProjectStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}