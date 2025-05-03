package com.example.cerbo.controller;

import com.example.cerbo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/investigateur")
public class InvestigateurController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/home")
    public ResponseEntity<?> home() {
        // Vous pouvez ajouter une logique spécifique ici si nécessaire
        return ResponseEntity.ok().build();
    }
}
