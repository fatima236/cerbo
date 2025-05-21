package com.example.cerbo.repository;

import com.example.cerbo.entity.MeetingAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingAttendeeRepository extends JpaRepository<MeetingAttendee, Long> {

    @Query("SELECT ma FROM MeetingAttendee ma " +
            "JOIN FETCH ma.user u " +
            "LEFT JOIN FETCH ma.relatedProject " +
            "WHERE ma.meeting.id = :meetingId")
    List<MeetingAttendee> findByMeetingId(@Param("meetingId") Long meetingId);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    void deleteByMeetingIdAndUserId(Long meetingId, Long userId);

    @Query("SELECT ma FROM MeetingAttendee ma " +
            "WHERE ma.meeting.id = :meetingId " +
            "AND ma.addedManually = false")
    List<MeetingAttendee> findAutomaticAttendeesByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT ma FROM MeetingAttendee ma " +
            "WHERE ma.meeting.id = :meetingId " +
            "AND ma.user.id = :userId")
    MeetingAttendee findByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);
}