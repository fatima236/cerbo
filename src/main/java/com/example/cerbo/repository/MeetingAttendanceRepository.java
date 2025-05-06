package com.example.cerbo.repository;

import com.example.cerbo.entity.Meeting;
import com.example.cerbo.entity.MeetingAttendance;
import com.example.cerbo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingAttendanceRepository extends JpaRepository<MeetingAttendance, Long> {
    List<MeetingAttendance> findByMeetingId(Long meetingId);
    MeetingAttendance findByMeetingAndEvaluator(Meeting meeting, User evaluator);
    void deleteByEvaluator(User evaluator);
}