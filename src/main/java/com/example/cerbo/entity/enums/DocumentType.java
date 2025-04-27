package com.example.cerbo.entity.enums;

public enum DocumentType {
    DESCRIPTIF_PROJET("Descriptif du projet"),
    CONSIDERATION_ETHIQUE("Considérations éthiques"),
    FICHE_INFORMATION_FR("Fiche information FR"),
    FICHE_INFORMATION_AR("Fiche information AR"),
    FICHE_CONSENTEMENT_FR("Formulaire consentement FR"),
    FICHE_CONSENTEMENT_AR("Formulaire consentement AR"),
    ATTESTATION_ENGAGEMENT("Attestation d'engagement"),
    ATTESTATION_CNDP(""),
    CV_INVESTIGATEUR("CV investigateur"),
    SUPPORT_FORMATION(""),
    PIECE_JOINTE_ARTICLE(""),
    PROGRAMME_EVENEMENT(""),
    AUTRE("Autre document");

    private final String displayName;

    DocumentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}