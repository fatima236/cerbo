package com.example.cerbo.repository;

import com.example.cerbo.entity.Meeting;
import com.example.cerbo.entity.MeetingAttendance;
import com.example.cerbo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingAttendanceRepository extends JpaRepository<MeetingAttendance, Long> {

    @Query("SELECT COUNT(a) FROM MeetingAttendance a " +
            "WHERE a.evaluator.id = :evaluatorId " +
            "AND a.present = true " +
            "AND YEAR(a.meeting.date) = :year")
    int countPresences(@Param("evaluatorId") Long evaluatorId,
                       @Param("year") int year);

    @Query("SELECT COUNT(a) FROM MeetingAttendance a " +
            "WHERE a.evaluator.id = :evaluatorId " +
            "AND a.present = false " +
            "AND (a.justified = false OR a.justification IS NULL) " +
            "AND YEAR(a.meeting.date) = :year")
    int countUnjustifiedAbsences(@Param("evaluatorId") Long evaluatorId,
                                 @Param("year") int year);

    // New methods to fix the errors
    List<MeetingAttendance> findByMeetingId(Long meetingId);

    MeetingAttendance findByMeetingAndEvaluator(Meeting meeting, User evaluator);

    Optional<MeetingAttendance> findByMeetingIdAndEvaluatorId(Long meetingId, Long evaluatorId);

    @Modifying
    @Query("DELETE FROM MeetingAttendance a WHERE a.evaluator.id = :evaluatorId")
    void deleteAllByEvaluatorId(@Param("evaluatorId") Long evaluatorId);
    @Query("SELECT COUNT(a) FROM MeetingAttendance a WHERE " +
            "a.evaluator.id = :evaluatorId AND " +
            "a.present = true AND " +
            "YEAR(a.meeting.date) = :year")
    int countAnnualPresences(@Param("evaluatorId") Long evaluatorId,
                             @Param("year") int year);

    @Query("SELECT COUNT(a) FROM MeetingAttendance a WHERE " +
            "a.evaluator.id = :evaluatorId AND " +
            "a.present = false AND " +
            "(a.justified = false OR a.justification IS NULL) AND " +
            "YEAR(a.meeting.date) = :year")
    int countAnnualUnjustifiedAbsences(@Param("evaluatorId") Long evaluatorId,
                                       @Param("year") int year);

    @Modifying
    @Query("DELETE FROM MeetingAttendance ma WHERE ma.evaluator.id = :evaluatorId")
    void deleteByEvaluatorId(@Param("evaluatorId") Long evaluatorId);

    List<MeetingAttendance> findByMeetingIdAndPresentTrue(Long meetingId);

    void deleteByMeetingId(Long meetingId);
    void deleteByMeetingIdAndIsManualTrue(Long meetingId);

}