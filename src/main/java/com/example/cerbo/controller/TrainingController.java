package com.example.cerbo.controller;

import com.example.cerbo.entity.Event;
import com.example.cerbo.entity.Training;
import com.example.cerbo.repository.TrainingRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainings")
@CrossOrigin(origins = "http://localhost:3000")
@AllArgsConstructor
public class TrainingController {

    @Autowired
    private TrainingRepository trainingRepository;

    @GetMapping
    public ResponseEntity<List<Training>> getAllTrainings() {
        List<Training> trainings = trainingRepository.findAll();
        trainings.forEach(training -> {
            training.getDocuments().forEach(document -> {
                document.setTraining(null);
            });
        });
        return ResponseEntity.ok(trainings);
    }

    @PostMapping("/addTraining")
    public ResponseEntity<Training> addEvent(@Valid @RequestBody Training training) {
        return ResponseEntity.ok(trainingRepository.save(training));
    }



}
