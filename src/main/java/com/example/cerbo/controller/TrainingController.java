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
import java.util.Optional;

@RestController
@RequestMapping("/api/trainings")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@AllArgsConstructor
public class TrainingController {

    @Autowired
    private TrainingRepository trainingRepository;

    @GetMapping
    public ResponseEntity<List<Training>> getAllTrainings() {
        List<Training> trainings = trainingRepository.findAll();

        return ResponseEntity.ok(trainings);
    }

    @GetMapping("/training/{id}")
    public ResponseEntity<Training> getTraining(@PathVariable("id") Long id) {
        Training training = trainingRepository.findById(id).orElse(null);

        return ResponseEntity.ok(training);

    }

    @PutMapping("/UpdateTraining/{id}")
    public ResponseEntity<Training> updateTraining(@PathVariable("id") Long id, @RequestBody Training training) {
        Optional<Training> existingTrainingOpt = trainingRepository.findById(id);

        if (existingTrainingOpt.isPresent()) {
            Training existingTraining = existingTrainingOpt.get();

            existingTraining.setTitle(training.getTitle());
            existingTraining.setDescription(training.getDescription());
            existingTraining.setStartDate(training.getStartDate());
            existingTraining.setEndDate(training.getEndDate());
            existingTraining.setLocation(training.getLocation());
            existingTraining.setAvailableSeats(training.getAvailableSeats());
            existingTraining.setRegistrationRequired(training.getRegistrationRequired());
            existingTraining.setRegistrationLink(training.getRegistrationLink());
            existingTraining.setOrganizer(training.getOrganizer());


            Training updatedTraining = trainingRepository.save(existingTraining);

            return ResponseEntity.ok(updatedTraining);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping("/addTraining")
    public ResponseEntity<Training> addTraining(@Valid @RequestBody Training training) {
        return ResponseEntity.ok(trainingRepository.save(training));
    }

    @DeleteMapping("/deleteTraining/{id}")
    public ResponseEntity<Void> deleteTraining(@PathVariable("id") Long trainingId) {
        if (!trainingRepository.existsById(trainingId)) {
            return ResponseEntity.notFound().build();
        }

        trainingRepository.deleteById(trainingId);
        return ResponseEntity.noContent().build();
    }
}
