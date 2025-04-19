package com.example.cerbo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/evaluateur")
public class EvaluateurController {

    @GetMapping("/home")
    public ResponseEntity<?> home() {
        // Vous pouvez ajouter une logique spécifique ici si nécessaire
        return ResponseEntity.ok().build();
    }
}