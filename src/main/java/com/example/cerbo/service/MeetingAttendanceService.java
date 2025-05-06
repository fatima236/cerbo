package com.example.cerbo.service;

import com.example.cerbo.entity.Meeting;
import com.example.cerbo.entity.MeetingAttendance;
import com.example.cerbo.entity.User;
import com.example.cerbo.exception.NotFoundException;
import com.example.cerbo.repository.MeetingAttendanceRepository;
import com.example.cerbo.repository.MeetingRepository;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingAttendanceService {
    private final MeetingAttendanceRepository attendanceRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

    public List<MeetingAttendance> getAttendancesForMeeting(Long meetingId) {
        return attendanceRepository.findByMeetingId(meetingId);
    }

    public List<User> getAllEvaluators() {
        // Récupère tous les utilisateurs et filtre ceux qui ont le rôle EVALUATEUR
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles() != null && user.getRoles().contains("EVALUATEUR"))
                .collect(Collectors.toList());
    }

    @Transactional
    public MeetingAttendance markAttendance(Long meetingId, Long evaluatorId, boolean present, String justification) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("Meeting not found"));
        User evaluator = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new NotFoundException("Evaluator not found"));

        MeetingAttendance attendance = attendanceRepository.findByMeetingAndEvaluator(meeting, evaluator);

        if (attendance == null) {
            attendance = new MeetingAttendance();
            attendance.setMeeting(meeting);
            attendance.setEvaluator(evaluator);
        }

        attendance.setPresent(present);
        attendance.setJustification(justification);

        return attendanceRepository.save(attendance);
    }

    @Transactional
    public void removeEvaluator(Long evaluatorId) {
        User evaluator = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new NotFoundException("Evaluator not found"));
        attendanceRepository.deleteByEvaluator(evaluator);
        userRepository.delete(evaluator);
    }
}