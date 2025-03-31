package com.example.cerbo;

import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Event;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

@SpringBootApplication
public class CerboApplication {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EventRepository eventRepository;

    public static void main(String[] args) {
        SpringApplication.run(CerboApplication.class, args);

    }


    @Bean
    public CommandLineRunner init() {
        return args -> {
        };
    }



}
