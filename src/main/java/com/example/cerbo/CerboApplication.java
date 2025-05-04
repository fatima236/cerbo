package com.example.cerbo;

import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Event;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.EventRepository;
import com.example.cerbo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@SpringBootApplication
public class CerboApplication {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    public static void main(String[] args) {
        SpringApplication.run(CerboApplication.class, args);

    }


    @Bean
    public CommandLineRunner init() {
        return args -> {
            User evaluator = new User();
            evaluator.setEmail("evaluateur2@example.com");
            evaluator.setPassword(passwordEncoder.encode("password123"));
            evaluator.setRoles(Set.of("EVALUATEUR"));  // Important : définir le rôle
            evaluator.setValidated(true);              // Important : activer l'utilisateur

            userRepository.save(evaluator);

        };
    }



}
