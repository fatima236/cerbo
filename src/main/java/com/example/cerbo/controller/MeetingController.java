package com.example.cerbo.controller;

import com.example.cerbo.entity.Meeting;
import com.example.cerbo.service.MeetingService;
import org.springframework.core.io.Resource;
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
    public ResponseEntity<Meeting> updateMeeting(
            @PathVariable Long id,
            @RequestBody Meeting meetingUpdates) {

        // Transmettre tous les champs modifiables au service
        return ResponseEntity.ok(meetingService.updateMeeting(id, meetingUpdates));
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
}