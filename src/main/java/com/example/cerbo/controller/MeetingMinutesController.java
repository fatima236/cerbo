package com.example.cerbo.controller;

import com.example.cerbo.dto.UserDTO;
import com.example.cerbo.dto.meeting.DecisionDTO;
import com.example.cerbo.dto.meeting.MeetingAttendanceDTO;
import com.example.cerbo.dto.meeting.MeetingDTO;
import com.example.cerbo.dto.meeting.MeetingMinutesDTO;
import com.example.cerbo.entity.Meeting;
import com.example.cerbo.entity.MeetingAttendance;
import com.example.cerbo.entity.User;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentReviewRepository;
import com.example.cerbo.repository.MeetingAttendanceRepository;
import com.example.cerbo.repository.MeetingRepository;
import com.example.cerbo.service.MeetingMinutesService;
import com.example.cerbo.service.PdfGenerationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/meeting-minutes")
@RequiredArgsConstructor
public class MeetingMinutesController {
    private final MeetingMinutesService minutesService;
    private final PdfGenerationService pdfGenerationService;
    private final MeetingRepository meetingRepository;
    private final MeetingAttendanceRepository attendanceRepository;
    private final DocumentReviewRepository documentReviewRepository;


    @GetMapping("/{meetingId}")
    public ResponseEntity<MeetingMinutesDTO> getMeetingMinutes(@PathVariable Long meetingId) {
        return ResponseEntity.ok(minutesService.generateMeetingMinutes(meetingId));
    }

    @PatchMapping("/{meetingId}")
    public ResponseEntity<MeetingDTO> updateMeetingSession(
            @PathVariable Long meetingId,
            @RequestBody Map<String, String> request) {

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réunion non trouvée"));

        meeting.setSession(request.get("session"));
        Meeting updatedMeeting = meetingRepository.save(meeting);

        return ResponseEntity.ok(MeetingDTO.createComplete(updatedMeeting));
    }

    @PatchMapping("/{meetingId}/session")
    public ResponseEntity<Void> updateSession(
            @PathVariable Long meetingId,
            @RequestBody Map<String, String> request) {

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réunion non trouvée"));

        meeting.setSession(request.get("session"));
        meetingRepository.save(meeting);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{meetingId}/present-members")
    public ResponseEntity<Void> savePresentMembers(
            @PathVariable Long meetingId,
            @RequestBody List<MeetingMinutesDTO.AttendeeInfo> attendeeInfos) {
        minutesService.savePresentMembers(meetingId, attendeeInfos);
        return ResponseEntity.ok().build();
    }

    @Data
    static class PresentMemberRequest {
        private String fullName;
        private String role;
    }

    // Sauvegarde des décisions pour les projets
    @PostMapping("/{meetingId}/project-decisions")
    public ResponseEntity<Void> saveProjectDecisions(
            @PathVariable Long meetingId,
            @RequestBody List<DecisionDTO> decisions) {
        minutesService.saveProjectDecisions(meetingId, decisions);
        return ResponseEntity.ok().build();
    }

    // Sauvegarde des décisions pour les réponses
    @PostMapping("/{meetingId}/response-decisions")
    public ResponseEntity<Void> saveResponseDecisions(
            @PathVariable Long meetingId,
            @RequestBody Map<String, String> decisions) {
        if (decisions == null) {
            return ResponseEntity.badRequest().build();
        }
        minutesService.saveResponseDecisions(meetingId, decisions);
        return ResponseEntity.ok().build();
    }

    // Génération du PDF
    @GetMapping("/{meetingId}/pdf")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long meetingId) {
        try {
            MeetingMinutesDTO minutes = minutesService.generateMeetingMinutes(meetingId);
            byte[] pdfBytes = pdfGenerationService.generateMinutesPdf(minutes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("pv_reunion_" + meetingId + ".pdf")
                            .build());

            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur lors de la génération: " + e.getMessage()).getBytes());
        }
    }

    @GetMapping("/document-reviews/final-examiners")
    public ResponseEntity<List<UserDTO>> getFinalExaminers(
            @RequestParam Long meetingId) {

        List<User> examiners = documentReviewRepository.findFinalExaminersByMeetingId(meetingId);
        List<UserDTO> dtos = examiners.stream()
                .map(user -> {
                    UserDTO dto = new UserDTO();
                    dto.setId(user.getId());
                    dto.setFirstName(user.getFullName());
                    dto.setLastName(user.getFullName());
                    // Compter le nombre de projets évalués
                    long projectCount = documentReviewRepository.countByReviewerAndFinalSubmissionAndProjectMeetingId(
                            user, true, meetingId);
                    dto.setProjectCount(projectCount);
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{meetingId}/attendees")
    public ResponseEntity<List<MeetingAttendanceDTO>> getMeetingAttendees(@PathVariable Long meetingId) {
        List<MeetingAttendance> attendances = attendanceRepository.findByMeetingId(meetingId);
        List<MeetingAttendanceDTO> dtos = attendances.stream()
                .map(MeetingAttendanceDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{meetingId}/attendees")
    public ResponseEntity<Void> saveAttendees(
            @PathVariable Long meetingId,
            @RequestBody List<MeetingAttendanceDTO> attendees) {

        minutesService.saveAttendees(meetingId, attendees);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Une erreur est survenue: " + e.getMessage());
    }
}