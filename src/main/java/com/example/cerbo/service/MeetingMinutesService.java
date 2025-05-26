package com.example.cerbo.service;

import com.example.cerbo.dto.meeting.DecisionDTO;
import com.example.cerbo.dto.meeting.MeetingAttendanceDTO;
import com.example.cerbo.dto.meeting.MeetingMinutesDTO;
import com.example.cerbo.entity.*;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.entity.enums.ReportStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MeetingMinutesService {
    private static final Logger logger = LoggerFactory.getLogger(MeetingMinutesService.class);

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final DocumentReviewRepository documentReviewRepository;
    private final ProjectRepository projectRepository;
    private final MeetingAttendanceRepository attendanceRepository;

    public MeetingMinutesDTO generateMeetingMinutes(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réunion non trouvée"));

        MeetingMinutesDTO dto = new MeetingMinutesDTO();
        dto.setMeetingId(meeting.getId());
        dto.setSession(meeting.getSession());
        dto.setMeetingDate(meeting.getDate());

        // 1. Récupérer tous les membres présents (automatiques + manuels)
        List<MeetingAttendance> attendances = attendanceRepository.findByMeetingIdAndPresentTrue(meetingId);

        // 2. Membres présents (évaluateurs + manuels)
        List<MeetingMinutesDTO.AttendeeInfo> presentMembers = attendances.stream()
                .map(attendance -> {
                    MeetingMinutesDTO.AttendeeInfo info = new MeetingMinutesDTO.AttendeeInfo();
                    info.setManual(attendance.isManual());

                    if (attendance.isManual()) {
                        info.setFullName(attendance.getManualMember());
                        info.setRole("Membre CERBO");
                    } else {
                        info.setUserId(attendance.getEvaluator().getId());
                        info.setFullName(attendance.getEvaluator().getFullName());
                        info.setRole("Examinateur CERBO");
                    }
                    return info;
                })
                .collect(Collectors.toList());

        dto.setPresentMembers(presentMembers);

        // 3. Examinateurs finaux (pour information)
        dto.setExaminers(getFinalExaminers(meeting));
        dto.setReviewedProjects(getReviewedProjects(meeting));
        dto.setInvestigatorResponses(getInvestigatorResponses(meeting));

        return dto;
    }

    private List<MeetingMinutesDTO.AttendeeInfo> getFinalExaminers(Meeting meeting) {
        // Récupérer uniquement les évaluateurs avec des soumissions finales pour les projets de cette réunion
        List<User> finalExaminers = documentReviewRepository.findFinalExaminersByMeetingId(meeting.getId());

        return finalExaminers.stream()
                .map(user -> {
                    MeetingMinutesDTO.AttendeeInfo info = new MeetingMinutesDTO.AttendeeInfo();
                    info.setUserId(user.getId());
                    info.setFullName(user.getFullName());
                    info.setRole("Examinateur final");
                    info.setManual(false);
                    return info;
                })
                .collect(Collectors.toList());
    }

    private List<MeetingMinutesDTO.AttendeeInfo> getExaminers(Meeting meeting) {
        // Remplacer la méthode existante par :
        return documentReviewRepository.findFinalExaminersByMeetingId(meeting.getId()).stream()
                .map(user -> {
                    MeetingMinutesDTO.AttendeeInfo info = new MeetingMinutesDTO.AttendeeInfo();
                    info.setUserId(user.getId());
                    info.setFullName(user.getFullName());
                    info.setRole("Examinateur CERBO");
                    info.setManual(false); // Indique que c'est généré automatiquement
                    return info;
                })
                .collect(Collectors.toList());
    }

    private List<MeetingMinutesDTO.AttendeeInfo> getPresentMembers(Meeting meeting) {
        return attendanceRepository.findByMeetingIdAndPresentTrue(meeting.getId()).stream()
                .map(attendance -> {
                    MeetingMinutesDTO.AttendeeInfo info = new MeetingMinutesDTO.AttendeeInfo();
                    info.setUserId(attendance.getEvaluator().getId());
                    info.setFullName(attendance.getEvaluator().getFullName());
                    info.setRole(String.join(", ", attendance.getEvaluator().getRoles()));
                    return info;
                })
                .collect(Collectors.toList());
    }

    private List<MeetingMinutesDTO.ProjectReview> getReviewedProjects(Meeting meeting) {
        return meeting.getAgendaItems().stream()
                .map(item -> {
                    MeetingMinutesDTO.ProjectReview review = new MeetingMinutesDTO.ProjectReview();
                    Project project = item.getProject();
                    review.setReference(project.getReference());
                    review.setTitle(project.getTitle());
                    review.setPrincipalInvestigator(project.getPrincipalInvestigator().getFullName());
                    review.setDecision(item.getDecision());
                    return review;
                })
                .collect(Collectors.toList());
    }

    private List<MeetingMinutesDTO.InvestigatorResponse> getInvestigatorResponses(Meeting meeting) {
        return meeting.getAgendaItems().stream()
                .filter(item -> {
                    Project project = item.getProject();
                    // Vérifiez si le projet a un rapport avec status RESPONDED
                    return project.getReports().stream()
                            .anyMatch(report -> report.getStatus() == ReportStatus.RESPONDED);
                })
                .map(item -> {
                    Project project = item.getProject();
                    MeetingMinutesDTO.InvestigatorResponse response = new MeetingMinutesDTO.InvestigatorResponse();
                    response.setReference(project.getReference());
                    response.setTitle(project.getTitle());
                    response.setPrincipalInvestigator(project.getPrincipalInvestigator().getFullName());
                    response.setDecision(item.getDecision());

                    // Utilisez la date du dernier rapport répondue
                    project.getReports().stream()
                            .filter(report -> report.getStatus() == ReportStatus.RESPONDED)
                            .max(Comparator.comparing(Report::getResponseDate))
                            .ifPresent(report -> response.setResponseDate(report.getResponseDate()));

                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveProjectDecisions(Long meetingId, List<DecisionDTO> decisions) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réunion non trouvée"));

        decisions.forEach(decision -> {
            meeting.getAgendaItems().stream()
                    .filter(item -> item.getProject().getReference().equals(decision.getReference()))
                    .findFirst()
                    .ifPresent(item -> {
                        item.setDecision(decision.getDecision());
                        // Sauvegarder explicitement l'item
                        meetingRepository.save(meeting); // Cela devrait persister les changements
                    });
        });
    }

    @Transactional
    public void saveResponseDecisions(Long meetingId, Map<String, String> decisions) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réunion non trouvée"));

        // Update decisions for projects that have investigator responses
        meeting.getAgendaItems().forEach(item -> {
            Project project = item.getProject();
            // Check if this project has any responded reports
            boolean hasResponse = project.getReports().stream()
                    .anyMatch(report -> report.getStatus() == ReportStatus.RESPONDED);

            if (hasResponse && decisions.containsKey(project.getReference())) {
                item.setDecision(decisions.get(project.getReference()));
            }
        });
    }

    @Transactional
    public void savePresentMembers(Long meetingId, List<MeetingMinutesDTO.AttendeeInfo> presentMembers) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réunion non trouvée"));

        // 1. Supprimer tous les membres manuels existants
        attendanceRepository.deleteByMeetingIdAndIsManualTrue(meetingId);

        // 2. Sauvegarder les nouveaux membres
        presentMembers.forEach(member -> {
            if (member.isManual()) {
                // Pour les membres manuels
                MeetingAttendance attendance = new MeetingAttendance();
                attendance.setMeeting(meeting);
                attendance.setPresent(true);
                attendance.setManual(true);
                attendance.setManualMember(member.getFullName());
                attendanceRepository.save(attendance);
            }
            // Les évaluateurs sont gérés automatiquement, pas besoin de les sauvegarder ici
        });
    }
    @Transactional
    public void saveAttendees(Long meetingId, List<MeetingAttendanceDTO> attendees) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réunion non trouvée"));

        // Supprimer les présences existantes
        attendanceRepository.deleteByMeetingId(meetingId);

        // Sauvegarder les nouvelles présences
        attendees.forEach(dto -> {
            MeetingAttendance attendance = new MeetingAttendance();
            attendance.setMeeting(meeting);
            attendance.setPresent(dto.isPresent());
            attendance.setJustification(dto.getJustification());
            attendance.setJustified(dto.isJustified());
            attendance.setManual(dto.isManual());

            User user = userRepository.findById(dto.getEvaluatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
            attendance.setEvaluator(user);

            attendanceRepository.save(attendance);
        });
    }
}