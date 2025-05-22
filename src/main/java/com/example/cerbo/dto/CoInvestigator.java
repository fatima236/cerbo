package com.example.cerbo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoInvestigator {
    private String name;
    private String surname;
    private String title;
    private String email;
    private String affiliation;
    private String address;
}