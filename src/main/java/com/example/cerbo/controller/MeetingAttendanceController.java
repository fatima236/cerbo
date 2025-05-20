package com.example.cerbo.controller;

import com.example.cerbo.dto.EvaluatorStatsResponse;
import com.example.cerbo.repository.MeetingRepository;
import com.example.cerbo.repository.MeetingAttendanceRepository;
import com.example.cerbo.repository.UserRepository;
import com.example.cerbo.entity.Meeting;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.MeetingAttendance;
import com.example.cerbo.service.MeetingAttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingAttendanceController {
    private final MeetingAttendanceService attendanceService;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingAttendanceRepository attendanceRepository;

    @GetMapping("/{meetingId}/attendance")
    public ResponseEntity<List<MeetingAttendance>> getMeetingAttendances(@PathVariable Long meetingId) {
        return ResponseEntity.ok(attendanceService.getAttendancesForMeeting(meetingId));
    }

    @GetMapping("/evaluators")
    public ResponseEntity<List<User>> getAllEvaluators() {
        return ResponseEntity.ok(attendanceService.getAllEvaluators());
    }

    @PostMapping("/{meetingId}/attendance")
    public ResponseEntity<MeetingAttendance> markAttendance(
            @PathVariable Long meetingId,
            @RequestParam Long evaluatorId,
            @RequestParam boolean present,
            @RequestParam(required = false) String justification) {
        return ResponseEntity.ok(
                attendanceService.markAttendance(meetingId, evaluatorId, present, justification)
        );
    }

    @DeleteMapping("/evaluators/{evaluatorId}")
    public ResponseEntity<Void> removeEvaluator(@PathVariable Long evaluatorId) {
        attendanceService.removeEvaluator(evaluatorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/evaluators/stats")
    public ResponseEntity<List<EvaluatorStatsResponse>> getEvaluatorsStats(
            @RequestParam int year) {

        List<User> evaluators = userRepository.findByRole("EVALUATEUR");
        final int TOTAL_MEETINGS = 11; // Fixé à 11 réunions annuelles

        List<EvaluatorStatsResponse> stats = evaluators.stream()
                .map(evaluator -> {
                    int presences = attendanceRepository.countAnnualPresences(evaluator.getId(), year);
                    int unjustifiedAbsences = attendanceRepository.countAnnualUnjustifiedAbsences(evaluator.getId(), year);

                    return new EvaluatorStatsResponse(
                            evaluator.getId(),
                            evaluator.getNom(),
                            evaluator.getPrenom(),
                            evaluator.getEmail(),
                            presences,
                            unjustifiedAbsences,
                            TOTAL_MEETINGS
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{meetingId}/attendance/initialize-from-invitations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> initializeAttendanceFromInvitations(@PathVariable Long meetingId) {
        try {
            int createdCount = attendanceService.initializeAttendanceFromInvitations(meetingId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Présences initialisées avec succès",
                    "createdCount", createdCount
            ));
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation des présences", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur lors de l'initialisation des présences: " + e.getMessage()
            ));
        }
    }
}