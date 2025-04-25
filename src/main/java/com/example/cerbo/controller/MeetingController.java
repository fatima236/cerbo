package com.example.cerbo.controller;

import com.example.cerbo.entity.Meeting;
import com.example.cerbo.service.MeetingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @GetMapping
    public ResponseEntity<List<Meeting>> getMeetingsByYear(@RequestParam int year) {
        return ResponseEntity.ok(meetingService.getMeetingsByYear(year));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Meeting> updateMeeting(
            @PathVariable Long id,
            @RequestBody Meeting meetingUpdates) {

        // Créer un objet Meeting avec seulement les champs modifiables
        Meeting updates = new Meeting();
        updates.setStatus(meetingUpdates.getStatus()); // Seul le statut peut être modifié

        return ResponseEntity.ok(meetingService.updateMeeting(id, updates));
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
}