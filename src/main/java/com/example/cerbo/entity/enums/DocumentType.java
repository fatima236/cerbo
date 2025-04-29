package com.example.cerbo.entity.enums;

public enum DocumentType {
    PROJECT_DESCRIPTION("Descriptif du projet"),
    ETHICAL_CONSIDERATIONS("Considérations éthiques"),
    INFORMATION_SHEET_FR("Fiche information française"),
    INFORMATION_SHEET_AR("Fiche information arabe"),
    CONSENT_FORM_FR("Formulaire consentement français"),
    CONSENT_FORM_AR("Formulaire consentement arabe"),
    COMMITMENT_CERTIFICATE("Attestation d'engagement"),
    INVESTIGATOR_CV("CV investigateur"),
    OTHER("Autre document");


    private final String displayName;

    DocumentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}