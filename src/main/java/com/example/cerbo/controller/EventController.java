package com.example.cerbo.controller;

import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Event;
import com.example.cerbo.entity.Training;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.EventRepository;
import com.example.cerbo.service.FileStorageService;
import com.example.cerbo.service.documentService.DocumentService;
import com.example.cerbo.service.eventService.EventService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@AllArgsConstructor
public class EventController {

    private final EventRepository eventRepository;
    private final EventService eventService;
    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        List<Event> events = eventRepository.findAll();

        return ResponseEntity.ok(events);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<Event> getEventById(@PathVariable Long eventId) {

        Event event = eventRepository.findById(eventId).orElse(null);

        return ResponseEntity.ok(event);
    }

    @PostMapping("/addEvent")
    public ResponseEntity<Event> addEvent(@RequestParam("title") String title,
                                          @RequestParam("description") String description,
                                          @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                          @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                          @RequestParam("location") String location,
                                          @RequestParam("type") String type,
                                          @RequestParam("organizer") String organizer,
                                          @RequestParam("file") MultipartFile file){
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setStartDate(startDate);
        event.setEndDate(endDate);
        event.setLocation(location);
        event.setType(type);
        event.setOrganizer(organizer);

        String filename = fileStorageService.storeFile(file);
        event.setFilename(filename);

        return ResponseEntity.ok(eventRepository.save(event));

    }

    @DeleteMapping("/deleteEvent/{id}")
    public ResponseEntity<Void> deleteTraining(@PathVariable("id") Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }
        fileStorageService.deleteFile(event.getFilename());
        eventRepository.delete(event);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/updateEvent/{eventId}")
    public ResponseEntity<Event> updateTraining(@PathVariable("eventId") Long eventId,
                                                @RequestParam("title") String title,
                                                @RequestParam("description") String description,
                                                @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                                @RequestParam("location") String location,
                                                @RequestParam("type") String type,
                                                @RequestParam("organizer") String organizer,
                                                @RequestParam(value = "image", required = false) MultipartFile image) {

        Optional<Event> existingEventOpt = eventRepository.findById(eventId);

        if (existingEventOpt.isPresent()) {
            Event existingEvent = existingEventOpt.get();

            existingEvent.setTitle(title);
            existingEvent.setDescription(description);
            existingEvent.setStartDate(startDate);
            existingEvent.setEndDate(endDate);
            existingEvent.setLocation(location);
            existingEvent.setType(type);
            existingEvent.setOrganizer(organizer);

            if (image != null && !image.isEmpty()) {
                fileStorageService.deleteFile(existingEvent.getFilename());
                String filename = fileStorageService.storeFile(image);
                existingEvent.setFilename(filename);

            }

            Event updatedEvent = eventRepository.save(existingEvent);
            return ResponseEntity.ok(updatedEvent);
        } else {
            return ResponseEntity.notFound().build();
        }
    }




}
