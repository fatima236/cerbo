package com.example.cerbo.controller;

import com.example.cerbo.entity.Meeting;
import com.example.cerbo.service.MeetingService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @GetMapping
    public ResponseEntity<List<Meeting>> getMeetingsByYear(
            @RequestParam int year,
            @RequestParam(required = false) Integer month) {

        if (month != null) {
            return ResponseEntity.ok(meetingService.getMeetingsByYearAndMonth(year, month));
        } else {
            return ResponseEntity.ok(meetingService.getMeetingsByYear(year));
        }
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateMeeting(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        try {
            Meeting meeting = meetingService.getMeetingById(id);
            if (meeting == null) {
                return ResponseEntity.notFound().build();
            }

            // Validation et mise à jour de la date - CORRECTION ICI
            if (updates.containsKey("date")) {
                String dateStr = (String) updates.get("date");

                // Parser la date au format YYYY-MM-DD
                LocalDate newDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);

                // Validation : pas de date dans le passé (sauf si admin veut forcer)
                if (newDate.isBefore(LocalDate.now()) && !Boolean.TRUE.equals(updates.get("force"))) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "La date ne peut pas être dans le passé",
                            "suggestion", "Utilisez 'force: true' pour forcer la modification"
                    ));
                }

                meeting.setDate(newDate);
            }

            // Mise à jour de l'heure
            if (updates.containsKey("time")) {
                String timeStr = (String) updates.get("time");
                LocalTime newTime = LocalTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_TIME);
                meeting.setTime(newTime);
            }

            Meeting updatedMeeting = meetingService.updateMeeting(meeting);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Réunion mise à jour avec succès",
                    "meeting", updatedMeeting
            ));

        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Format de date/heure invalide. Attendu: YYYY-MM-DD pour la date, HH:mm pour l'heure",
                    "details", e.getMessage()
            ));
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Long id) {
        meetingService.deleteMeeting(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generate")
    public ResponseEntity<List<Meeting>> generateMeetings(@RequestParam int year) {
        return ResponseEntity.ok(meetingService.generateMeetings(year));
    }
    // Dans MeetingController.java
    @GetMapping("/download-pdf")
    public ResponseEntity<Resource> downloadPdfPlanning(@RequestParam int year) {
        return meetingService.generatePdfPlanning(year);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Meeting> getMeetingById(@PathVariable Long id) {
        Meeting meeting = meetingService.getMeetingById(id);
        if (meeting == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(meeting);
    }



}