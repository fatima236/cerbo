package com.example.cerbo.service;

import com.example.cerbo.entity.Meeting;
import com.example.cerbo.entity.MeetingAttendance;
import com.example.cerbo.entity.MeetingAttendee;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.MeetingAttendanceRepository;
import com.example.cerbo.repository.MeetingAttendeeRepository;
import com.example.cerbo.repository.MeetingRepository;
import com.example.cerbo.repository.MeetingProjectRepository;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingAttendanceService {
    private final MeetingAttendanceRepository attendanceRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final MeetingAttendeeRepository meetingAttendeeRepository;
    private final MeetingProjectRepository meetingProjectRepository;
    private final MeetingService meetingService;

    public List<MeetingAttendance> getAttendancesForMeeting(Long meetingId) {
        return attendanceRepository.findByMeetingId(meetingId);
    }

    public List<User> getAllEvaluators() {
        return userRepository.findByRole("EVALUATEUR");
    }

    @Transactional
    public MeetingAttendance markAttendance(Long meetingId, Long evaluatorId, boolean present, String justification) {
        // Vérifier si l'entrée d'attendance existe déjà
        MeetingAttendance attendance = attendanceRepository.findByMeetingIdAndEvaluatorId(meetingId, evaluatorId)
                .orElseGet(() -> {
                    // Si non, en créer une nouvelle
                    Meeting meeting = meetingRepository.findById(meetingId)
                            .orElseThrow(() -> new RuntimeException("Meeting not found"));
                    User evaluator = userRepository.findById(evaluatorId)
                            .orElseThrow(() -> new RuntimeException("Evaluator not found"));

                    MeetingAttendance newAttendance = new MeetingAttendance();
                    newAttendance.setMeeting(meeting);
                    newAttendance.setEvaluator(evaluator);
                    return newAttendance;
                });

        // Mettre à jour les informations de présence
        attendance.setPresent(present);
        boolean justified = justification != null && !justification.trim().isEmpty();
        attendance.setJustified(justified);
        attendance.setJustification(justified ? justification : null);

        return attendanceRepository.save(attendance);
    }

    @Transactional
    public void removeEvaluator(Long evaluatorId) {
        // Supprimer les présences
        List<MeetingAttendance> attendances = attendanceRepository.findAll().stream()
                .filter(a -> a.getEvaluator().getId().equals(evaluatorId))
                .collect(Collectors.toList());

        attendanceRepository.deleteAll(attendances);

        // Supprimer l'évaluateur
        userRepository.deleteById(evaluatorId);
    }

    // ==== MÉTHODES DE GESTION DES PARTICIPANTS (anciennement dans MeetingAttendeeService) ====

    /**
     * Récupère tous les participants d'une réunion
     */
    @Transactional(readOnly = true)
    public List<MeetingAttendee> getMeetingAttendees(Long meetingId) {
        return meetingAttendeeRepository.findByMeetingId(meetingId);
    }

    /**
     * Met à jour les participants en fonction de l'ordre du jour
     */
    @Transactional
    public void updateAttendeesFromAgenda(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Réunion non trouvée"));

        // Récupérer tous les projets à l'ordre du jour
        List<Project> projects = meetingService.getAgendaProjects(meetingId);

        // Récupérer les évaluateurs actuellement invités
        List<MeetingAttendee> currentAttendees = meetingAttendeeRepository.findByMeetingId(meetingId);
        Map<Long, MeetingAttendee> currentAttendeeMap = currentAttendees.stream()
                .collect(Collectors.toMap(
                        a -> a.getUser().getId(),
                        a -> a,
                        (a1, a2) -> a1  // En cas de doublon, garder le premier
                ));

        // Garder trace des évaluateurs qui sont encore pertinents
        Set<Long> relevantEvaluatorIds = new HashSet<>();

        // Pour chaque projet, identifier les évaluateurs
        for (Project project : projects) {
            if (project.getReviewers() != null && !project.getReviewers().isEmpty()) {
                for (User reviewer : project.getReviewers()) {
                    relevantEvaluatorIds.add(reviewer.getId());

                    if (!currentAttendeeMap.containsKey(reviewer.getId())) {
                        // Nouvel évaluateur à ajouter
                        MeetingAttendee attendee = new MeetingAttendee();
                        attendee.setMeeting(meeting);
                        attendee.setUser(reviewer);
                        attendee.setAddedManually(false);
                        attendee.setRelatedProject(project);

                        meetingAttendeeRepository.save(attendee);
                    }
                }
            }
        }

        // Supprimer les évaluateurs automatiques qui ne sont plus pertinents
        List<MeetingAttendee> attendeesToRemove = currentAttendees.stream()
                .filter(a -> !a.getAddedManually())  // Seulement les automatiques
                .filter(a -> !relevantEvaluatorIds.contains(a.getUser().getId()))
                .collect(Collectors.toList());

        if (!attendeesToRemove.isEmpty()) {
            meetingAttendeeRepository.deleteAll(attendeesToRemove);
        }
    }

    /**
     * Ajoute un évaluateur manuellement à une réunion
     */
    @Transactional
    public void addAttendeeManually(Long meetingId, Long userId) {
        MeetingAttendee existingAttendee = meetingAttendeeRepository.findByMeetingIdAndUserId(meetingId, userId);

        if (existingAttendee != null) {
            if (!existingAttendee.getAddedManually()) {
                existingAttendee.setAddedManually(true);
                meetingAttendeeRepository.save(existingAttendee);
            } else {
                throw new IllegalArgumentException("L'évaluateur est déjà invité manuellement à cette réunion");
            }
            return;
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Réunion non trouvée"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        MeetingAttendee attendee = new MeetingAttendee();
        attendee.setMeeting(meeting);
        attendee.setUser(user);
        attendee.setAddedManually(true);

        meetingAttendeeRepository.save(attendee);
    }

    /**
     * Supprime un évaluateur d'une réunion
     */
    @Transactional
    public void removeAttendee(Long meetingId, Long userId) {
        MeetingAttendee attendee = meetingAttendeeRepository.findByMeetingIdAndUserId(meetingId, userId);

        if (attendee == null) {
            throw new IllegalArgumentException("L'évaluateur n'est pas invité à cette réunion");
        }

        meetingAttendeeRepository.delete(attendee);
    }

    /**
     * Initialise les présences à partir des invitations
     */
    @Transactional
    public int initializeAttendanceFromInvitations(Long meetingId) {
        // Récupérer les participants invités
        List<MeetingAttendee> invitedAttendees = meetingAttendeeRepository.findByMeetingId(meetingId);

        if (invitedAttendees.isEmpty()) {
            return 0;
        }

        // Vérifier quels participants ont déjà une entrée de présence
        List<MeetingAttendance> existingAttendances = attendanceRepository.findByMeetingId(meetingId);
        Set<Long> existingAttendeeIds = existingAttendances.stream()
                .map(attendance -> attendance.getEvaluator().getId())
                .collect(Collectors.toSet());

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Réunion non trouvée"));

        int createdCount = 0;

        // Créer des entrées de présence pour les participants invités qui n'en ont pas encore
        for (MeetingAttendee attendee : invitedAttendees) {
            User evaluator = attendee.getUser();

            if (!existingAttendeeIds.contains(evaluator.getId())) {
                MeetingAttendance attendance = new MeetingAttendance();
                attendance.setMeeting(meeting);
                attendance.setEvaluator(evaluator);
                attendance.setPresent(false); // Par défaut, absent jusqu'à indication contraire
                attendance.setJustified(false);
                attendance.setJustification(null);

                attendanceRepository.save(attendance);
                createdCount++;
            }
        }

        return createdCount;
    }
}