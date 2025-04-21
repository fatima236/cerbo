package com.example.cerbo.controller;

import com.example.cerbo.entity.User;
import com.example.cerbo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController  // Changé de @Controller à @RestController pour renvoyer directement du JSON
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/home")
    public ResponseEntity<?> home() {
        // Vous pouvez ajouter une logique spécifique ici si nécessaire
        return ResponseEntity.ok().build();
    }

    @GetMapping("/evaluators")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllEvaluators() {
        List<User> evaluators = userRepository.findAll().stream()
                .filter(user -> user.getRoles().contains("EVALUATEUR"))
                .collect(Collectors.toList());

        return ResponseEntity.ok(evaluators);
    }
}