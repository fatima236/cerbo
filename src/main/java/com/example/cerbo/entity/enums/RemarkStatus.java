package com.example.cerbo.entity.enums;

public enum RemarkStatus {
    PENDING("En attente"),
    DRAFT("Brouillon"),
    PENDING_VALIDATION("En attente de validation"),
    VALIDATED("Validée"),
    REJECTED("Rejetée"),
    MODIFIED("Modifiée"),
    REVIEWED("Relue");

    private final String label;

    RemarkStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
