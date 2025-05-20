package com.example.cerbo.controller;

import com.example.cerbo.entity.MeetingAttendee;
import com.example.cerbo.service.MeetingAttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/meetings/{meetingId}/attendees")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class MeetingAttendeeController {

    private final MeetingAttendanceService meetingAttendanceService;

    @GetMapping
    public ResponseEntity<?> getMeetingAttendees(@PathVariable Long meetingId) {
        try {
            List<MeetingAttendee> attendees = meetingAttendanceService.getMeetingAttendees(meetingId);

            List<Map<String, Object>> result = attendees.stream()
                    .map(attendee -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", attendee.getId());
                        map.put("userId", attendee.getUser().getId());
                        map.put("email", attendee.getUser().getEmail());
                        map.put("name", attendee.getUser().getPrenom() + " " + attendee.getUser().getNom());
                        map.put("addedManually", attendee.getAddedManually());

                        if (attendee.getRelatedProject() != null) {
                            map.put("relatedProjectId", attendee.getRelatedProject().getId());
                            map.put("relatedProjectTitle", attendee.getRelatedProject().getTitle());
                        }

                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des participants", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/users/{userId}")
    public ResponseEntity<?> addAttendee(
            @PathVariable Long meetingId,
            @PathVariable Long userId) {
        try {
            meetingAttendanceService.addAttendeeManually(meetingId, userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Évaluateur ajouté à la réunion"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout de l'évaluateur", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur interne du serveur"
            ));
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> removeAttendee(
            @PathVariable Long meetingId,
            @PathVariable Long userId) {
        try {
            meetingAttendanceService.removeAttendee(meetingId, userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Évaluateur retiré de la réunion"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'évaluateur", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur interne du serveur"
            ));
        }
    }

    @PostMapping("/update-from-agenda")
    public ResponseEntity<?> updateAttendeesFromAgenda(@PathVariable Long meetingId) {
        try {
            meetingAttendanceService.updateAttendeesFromAgenda(meetingId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Liste des évaluateurs mise à jour en fonction de l'ordre du jour"
            ));
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour des évaluateurs", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur interne du serveur"
            ));
        }
    }
}