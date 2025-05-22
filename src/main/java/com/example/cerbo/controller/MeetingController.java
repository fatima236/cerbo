package com.example.cerbo.controller;

import com.example.cerbo.dto.MeetingAttendeeDTO;
import com.example.cerbo.dto.MeetingDTO;
import com.example.cerbo.dto.MeetingProjectDTO;
import com.example.cerbo.dto.MeetingRequestDTO;
import com.example.cerbo.entity.Meeting;
import com.example.cerbo.service.MeetingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/meeting")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    /**
     * ✅ GET /api/meeting - Liste simplifiée pour le calendrier
     */
    @GetMapping
    public ResponseEntity<List<MeetingDTO>> getMeetingsByYear(
            @RequestParam int year,
            @RequestParam(required = false) Integer month) {

        try {
            List<Meeting> meetings;
            if (month != null) {
                meetings = meetingService.getMeetingsByYearAndMonth(year, month);
            } else {
                meetings = meetingService.getMeetingsByYear(year);
            }

            // ✅ VERSION SIMPLE pour les listes (évite de charger toutes les relations)
            List<MeetingDTO> meetingDTOs = MeetingDTO.fromEntityList(meetings);

            log.info("Récupération de {} réunions (DTOs simples) pour l'année {}", meetingDTOs.size(), year);

            return ResponseEntity.ok(meetingDTOs);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des réunions pour l'année {}: {}", year, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ✅ GET /api/meeting/{id} - Détails complets d'une réunion
     */
    @GetMapping("/{id}")
    public ResponseEntity<MeetingDTO> getMeetingById(@PathVariable Long id) {
        try {
            Meeting meeting = meetingService.getMeetingById(id);
            if (meeting == null) {
                return ResponseEntity.notFound().build();
            }

            // ✅ VERSION COMPLÈTE avec toutes les relations
            MeetingDTO meetingDTO = MeetingDTO.fromEntity(meeting);

            log.info("Récupération réunion complète ID: {} avec {} projets, {} participants",
                    id, meetingDTO.getAgendaItemsCount(), meetingDTO.getAttendeesCount());

            return ResponseEntity.ok(meetingDTO);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la réunion ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ✅ PUT /api/meeting/{id} - Mise à jour (inchangé)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateMeeting(
            @PathVariable Long id,
            @Valid @RequestBody MeetingRequestDTO meetingRequest) {

        try {
            log.info("Mise à jour de la réunion ID: {} avec les données: {}", id, meetingRequest);

            Meeting existingMeeting = meetingService.getMeetingById(id);
            if (existingMeeting == null) {
                return ResponseEntity.notFound().build();
            }

            // Validation business
            if (meetingRequest.getDate().isBefore(LocalDate.now()) && !meetingRequest.getForce()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Impossible de modifier une réunion dont la date est passée",
                                "canForce", true
                        ));
            }

            // Mise à jour
            existingMeeting.setDate(meetingRequest.getDate());
            existingMeeting.setTime(meetingRequest.getTime());
            existingMeeting.setStatus(meetingRequest.getStatus());
            existingMeeting.setMonth(meetingRequest.getMonth());

            Meeting updatedMeeting = meetingService.saveMeeting(existingMeeting);

            // ✅ RETOURNER VERSION SIMPLE pour les listes
            MeetingDTO responseDTO = MeetingDTO.fromEntitySimple(updatedMeeting);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Réunion mise à jour avec succès",
                    "meeting", responseDTO
            ));

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de la réunion ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne du serveur"));
        }
    }

    /**
     * ✅ POST /api/meeting/generate - Génération (inchangé)
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generateMeetings(@RequestParam int year) {
        try {
            log.info("Génération du planning pour l'année: {}", year);

            meetingService.generateMeetings(year);
            List<Meeting> generatedMeetings = meetingService.getMeetingsByYear(year);
            List<MeetingDTO> meetingDTOs = MeetingDTO.fromEntityList(generatedMeetings);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Planning généré avec succès",
                    "meetings", meetingDTOs,
                    "count", meetingDTOs.size()
            ));
        } catch (Exception e) {
            log.error("Erreur lors de la génération du planning pour l'année {}: {}", year, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la génération du planning"));
        }
    }

//    // ✅ NOUVEAU ENDPOINT pour récupérer les projets d'agenda d'une réunion
//    @GetMapping("/{id}/agenda")
//    public ResponseEntity<List<MeetingProjectDTO>> getMeetingAgenda(@PathVariable Long id) {
//        try {
//            Meeting meeting = meetingService.getMeetingById(id);
//            if (meeting == null) {
//                return ResponseEntity.notFound().build();
//            }
//
//            List<MeetingProjectDTO> agendaDTO = MeetingProjectDTO.fromEntityList(meeting.getAgendaItems());
//
//            return ResponseEntity.ok(agendaDTO);
//        } catch (Exception e) {
//            log.error("Erreur lors de la récupération de l'agenda pour la réunion ID {}: {}", id, e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }

//    // ✅ NOUVEAU ENDPOINT pour récupérer les participants d'une réunion
//    @GetMapping("/{id}/attendees")
//    public ResponseEntity<List<MeetingAttendeeDTO>> getMeetingAttendees(@PathVariable Long id) {
//        try {
//            Meeting meeting = meetingService.getMeetingById(id);
//            if (meeting == null) {
//                return ResponseEntity.notFound().build();
//            }
//
//            List<MeetingAttendeeDTO> attendeesDTO = MeetingAttendeeDTO.fromEntityList(meeting.getAttendees());
//
//            return ResponseEntity.ok(attendeesDTO);
//        } catch (Exception e) {
//            log.error("Erreur lors de la récupération des participants pour la réunion ID {}: {}", id, e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
}