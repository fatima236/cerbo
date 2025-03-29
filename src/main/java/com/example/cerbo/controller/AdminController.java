package com.example.cerbo.controller;



import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/home")
    public ResponseEntity<?> home() {
        // Vous pouvez ajouter une logique spécifique ici si nécessaire
        return ResponseEntity.ok().build();
    }
}