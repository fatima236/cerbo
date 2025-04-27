package com.example.cerbo.service;

import com.example.cerbo.entity.Meeting;
import com.example.cerbo.repository.MeetingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final EmailService emailService;

    public MeetingService(MeetingRepository meetingRepository, EmailService emailService) {
        this.meetingRepository = meetingRepository;
        this.emailService = emailService;
    }

    private Meeting createMeeting(int year, int month, int day, DayOfWeek dayOfWeek, int hour, int minute) {
        LocalDate date = LocalDate.of(year, month, day);

        if (date.getDayOfWeek() != dayOfWeek) {
            date = date.with(TemporalAdjusters.next(dayOfWeek));
        }

        Meeting meeting = new Meeting();
        meeting.setDate(date);
        meeting.setTime(LocalTime.of(hour, minute));
        meeting.setYear(year);
        meeting.setStatus("Planifiée");
        return meeting;
    }

    public List<Meeting> getMeetingsByYear(int year) {
        return meetingRepository.findByYear(year);
    }

    public Meeting updateMeeting(Long id, Meeting meetingDetails) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        // Mettre à jour tous les champs modifiables
        if (meetingDetails.getDate() != null) {
            meeting.setDate(meetingDetails.getDate());
        }

        if (meetingDetails.getTime() != null) {
            meeting.setTime(meetingDetails.getTime());
        }

        if (meetingDetails.getStatus() != null &&
                ("Planifiée".equals(meetingDetails.getStatus()) ||
                        "Annulée".equals(meetingDetails.getStatus()) ||
                        "Terminée".equals(meetingDetails.getStatus()))) {
            meeting.setStatus(meetingDetails.getStatus());
        }

        // Vérifier si la réunion est passée
        if (isPastMeeting(meeting)) {
            meeting.setStatus("Terminée");
        }

        return meetingRepository.save(meeting);
    }

    private boolean isPastMeeting(Meeting meeting) {
        LocalDate now = LocalDate.now();
        LocalDate meetingDate = meeting.getDate();

        if (meetingDate.isBefore(now)) {
            return true;
        }

        if (meetingDate.isEqual(now)) {
            LocalTime nowTime = LocalTime.now();
            LocalTime meetingTime = meeting.getTime();
            return meetingTime.isBefore(nowTime);
        }

        return false;
    }

    public void deleteMeeting(Long id) {
        meetingRepository.deleteById(id);
    }

    @Transactional
    public List<Meeting> generateMeetings(int year) {
        meetingRepository.deleteByYear(year);

        List<Meeting> meetings = Arrays.asList(
                createMeeting(year, 1, 23, DayOfWeek.THURSDAY, 15, 0),
                createMeeting(year, 2, 25, DayOfWeek.TUESDAY, 15, 0),
                createMeeting(year, 3, 26, DayOfWeek.WEDNESDAY, 13, 0),
                createMeeting(year, 4, 24, DayOfWeek.THURSDAY, 15, 0),
                createMeeting(year, 5, 26, DayOfWeek.MONDAY, 15, 0),
                createMeeting(year, 6, 24, DayOfWeek.TUESDAY, 15, 0),
                createMeeting(year, 7, 30, DayOfWeek.WEDNESDAY, 15, 0),
                createMeeting(year, 9, 25, DayOfWeek.THURSDAY, 15, 0),
                createMeeting(year, 10, 27, DayOfWeek.MONDAY, 15, 0),
                createMeeting(year, 11, 25, DayOfWeek.TUESDAY, 15, 0),
                createMeeting(year, 12, 31, DayOfWeek.WEDNESDAY, 15, 0)
        );

        List<Meeting> savedMeetings = new ArrayList<>();
        for (Meeting meeting : meetings) {
            try {
                if (isPastMeeting(meeting)) {
                    meeting.setStatus("Terminée");
                }
                savedMeetings.add(meetingRepository.save(meeting));
            } catch (Exception e) {
                System.err.println("Erreur sauvegarde réunion: " + e.getMessage());
            }
        }

        return savedMeetings;
    }

    @Scheduled(cron = "0 0 9 * * ?") // Exécuté tous les jours à 9h du matin
    public void checkMeetingReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Meeting> tomorrowMeetings = meetingRepository.findByDate(tomorrow);

        for (Meeting meeting : tomorrowMeetings) {
            String status = meeting.getStatus();
            String subject = "Rappel: Réunion " + (status.equals("Annulée") ? "ANNULÉE" : "prévue");
            String text = "La réunion du " + meeting.getDate() + " à " + meeting.getTime() +
                    " est " + (status.equals("Annulée") ? "annulée." : "prévue.");

            List<String> participantEmails = getParticipantEmailsForMeeting(meeting.getId());

            if (participantEmails != null && !participantEmails.isEmpty()) {
                for (String email : participantEmails) {
                    try {
                        emailService.sendReminderEmail(email, subject, text);
                    } catch (Exception e) {
                        System.err.println("Erreur lors de l'envoi du rappel à " + email + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private List<String> getParticipantEmailsForMeeting(Long meetingId) {
        // Implémentez cette méthode pour retourner la liste des emails des participants
        return Arrays.asList("participant1@example.com", "participant2@example.com");
    }
}