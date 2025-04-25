package com.example.cerbo.service;

import com.example.cerbo.entity.Meeting;
import com.example.cerbo.repository.MeetingRepository;
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

    public MeetingService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
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

        // Ne permettre que la modification du statut entre Planifiée et Annulée
        if (meetingDetails.getStatus() != null &&
                ("Planifiée".equals(meetingDetails.getStatus()) ||
                        "Annulée".equals(meetingDetails.getStatus()))) {
            meeting.setStatus(meetingDetails.getStatus());
        }

        // Vérifier si la réunion est passée pour mettre à jour automatiquement le statut
        if (isPastMeeting(meeting)) {
            meeting.setStatus("Terminée");
        }

        return meetingRepository.save(meeting);
    }

    private boolean isPastMeeting(Meeting meeting) {
        LocalDate now = LocalDate.now();
        LocalDate meetingDate = meeting.getDate();

        // Si la date est passée
        if (meetingDate.isBefore(now)) {
            return true;
        }

        // Si c'est aujourd'hui, vérifier l'heure
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
                // Vérifier si la réunion est passée avant de sauvegarder
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
}