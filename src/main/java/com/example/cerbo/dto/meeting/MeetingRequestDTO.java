package com.example.cerbo.dto.meeting;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO pour les requêtes de création et modification de réunions
 * Utilisé pour recevoir les données depuis le frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class MeetingRequestDTO {

    private Long id; // Null pour création, rempli pour mise à jour

    private String month;

    private String status;

    @NotNull(message = "L'année est obligatoire")
    @Min(value = 2020, message = "L'année doit être supérieure à 2020")
    @Max(value = 2030, message = "L'année doit être inférieure à 2030")
    private Integer year;

    @NotNull(message = "La date est obligatoire")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull(message = "L'heure est obligatoire")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime time;

    // Champ pour forcer les modifications (dates passées, etc.)
    private Boolean force = false;

    /**
     * Validation métier personnalisée
     */
    public boolean isValid() {
        if (year == null || year < 2020 || year > 2030) {
            log.warn("Année invalide: {}", year);
            return false;
        }

        if (date == null) {
            log.warn("Date manquante");
            return false;
        }

        if (time == null) {
            log.warn("Heure manquante");
            return false;
        }

        if (status == null || status.trim().isEmpty()) {
            log.warn("Statut manquant ou vide");
            return false;
        }

        return true;
    }

    /**
     * Méthode pour déterminer si c'est une création ou une mise à jour
     */
    public boolean isCreation() {
        return id == null;
    }

    /**
     * Méthode pour vérifier si la modification doit être forcée
     */
    public boolean shouldForceUpdate() {
        return force != null && force;
    }

    /**
     * Applique des valeurs par défaut pour les champs manquants
     */
    public void applyDefaults() {
        if (status == null || status.trim().isEmpty()) {
            status = "Planifiée";
        }

        if (force == null) {
            force = false;
        }

        // Génère automatiquement le mois si manquant
        if (month == null && date != null) {
            month = getMonthName(date.getMonthValue());
        }

        // Génère automatiquement l'année si manquante
        if (year == null && date != null) {
            year = date.getYear();
        }
    }

    /**
     * Convertit un numéro de mois en nom français
     */
    private String getMonthName(int monthValue) {
        String[] monthNames = {
                "", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
        };

        if (monthValue >= 1 && monthValue <= 12) {
            return monthNames[monthValue];
        }
        return "Mois inconnu";
    }

    /**
     * Valide que la date n'est pas trop ancienne (plus de 1 an)
     */
    public boolean isDateTooOld() {
        if (date == null) return false;

        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        return date.isBefore(oneYearAgo);
    }

    /**
     * Valide que la date n'est pas trop future (plus de 2 ans)
     */
    public boolean isDateTooFuture() {
        if (date == null) return false;

        LocalDate twoYearsLater = LocalDate.now().plusYears(2);
        return date.isAfter(twoYearsLater);
    }

    /**
     * Vérifie si la date est dans le passé
     */
    public boolean isInPast() {
        if (date == null) return false;
        return date.isBefore(LocalDate.now());
    }

    /**
     * Retourne un résumé pour les logs
     */
    public String getSummary() {
        return String.format("MeetingRequest[id=%s, date=%s, time=%s, status=%s, force=%s]",
                id, date, time, status, force);
    }

    /**
     * Méthode toString pour les logs
     */
    @Override
    public String toString() {
        return getSummary();
    }
}