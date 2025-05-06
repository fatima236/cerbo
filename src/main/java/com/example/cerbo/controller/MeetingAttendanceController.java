package com.example.cerbo.controller;

import com.example.cerbo.entity.MeetingAttendance;
import com.example.cerbo.entity.User;
import com.example.cerbo.service.MeetingAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingAttendanceController {
    private final MeetingAttendanceService attendanceService;

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
}