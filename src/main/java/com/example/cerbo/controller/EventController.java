package com.example.cerbo.controller;

import com.example.cerbo.entity.Event;
import com.example.cerbo.repository.EventRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:3000")
@AllArgsConstructor
public class EventController {

    private EventRepository eventRepository;

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        events.forEach(event -> {
            event.getDocuments().forEach(document -> {
                document.setEvent(null);  // Éliminer la référence à l'événement dans chaque document
            });
        });
        return ResponseEntity.ok(events);
    }

    @PostMapping("/addEvent")
    public ResponseEntity<Event> addEvent(@Valid @RequestBody Event event) {
        return ResponseEntity.ok(eventRepository.save(event));
    }


}
