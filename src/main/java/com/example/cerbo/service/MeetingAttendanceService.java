package com.example.cerbo.service;

import com.example.cerbo.entity.Meeting;
import com.example.cerbo.entity.MeetingAttendance;
import com.example.cerbo.entity.User;
import com.example.cerbo.exception.NotFoundException;
import com.example.cerbo.repository.MeetingAttendanceRepository;
import com.example.cerbo.repository.MeetingRepository;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;
import com.example.cerbo.annotation.Loggable;
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
        attendance.setJustified(justification != null && !justification.isEmpty());

        return attendanceRepository.save(attendance);
    }

    public int getUnjustifiedAbsenceCount(Long evaluatorId, int year) {
        return attendanceRepository.countUnjustifiedAbsences(evaluatorId, year);
    }

    @Scheduled(cron = "0 0 0 1 1 ?") // Exécuté le 1er janvier chaque année
    @Transactional
    public void removeEvaluatorsWithExcessiveAbsences() {
        int currentYear = Year.now().getValue() - 1;
        int maxUnjustifiedAbsences = 10;

        List<User> evaluators = getAllEvaluators();

        for (User evaluator : evaluators) {
            int absences = getUnjustifiedAbsenceCount(evaluator.getId(), currentYear);
            if (absences > maxUnjustifiedAbsences) {
                removeEvaluator(evaluator.getId());
            }
        }
    }

    @Transactional


    public void removeEvaluator(Long evaluatorId) {
        User evaluator = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new NotFoundException("Evaluator not found"));

        // 1. D'abord supprimer toutes les attendances liées
        attendanceRepository.deleteAllByEvaluatorId(evaluatorId);

        // 2. Puis supprimer l'utilisateur
        userRepository.delete(evaluator);

        // 3. Forcer le flush pour s'assurer que les opérations sont exécutées immédiatement
        userRepository.flush();
    }
    public int getAnnualPresenceCount(Long evaluatorId, int year) {
        return attendanceRepository.countAnnualPresences(evaluatorId, year);
    }

    public int getAnnualUnjustifiedAbsenceCount(Long evaluatorId, int year) {
        return attendanceRepository.countAnnualUnjustifiedAbsences(evaluatorId, year);
    }
}