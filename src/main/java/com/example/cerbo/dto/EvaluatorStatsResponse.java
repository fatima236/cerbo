package com.example.cerbo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EvaluatorStatsResponse {
    private Long evaluatorId;
    private String nom;
    private String prenom;
    private String email;
    private int presenceCount;
    private int unjustifiedAbsences;
    private int totalMeetings = 11; // Valeur fixe
}