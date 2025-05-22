package com.example.cerbo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
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

    // Champ pour forcer la mise à jour même si la date est dans le passé
    private Boolean force = false;
}