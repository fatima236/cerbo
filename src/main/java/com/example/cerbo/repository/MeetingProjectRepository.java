package com.example.cerbo.repository;

import com.example.cerbo.entity.MeetingProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingProjectRepository extends JpaRepository<MeetingProject, Long> {

    @Query("SELECT mp FROM MeetingProject mp " +
            "JOIN FETCH mp.project p " +
            "JOIN FETCH p.principalInvestigator " +
            "WHERE mp.meeting.id = :meetingId " +
            "ORDER BY mp.orderIndex ASC")
    List<MeetingProject> findByMeetingIdOrderByOrderIndex(@Param("meetingId") Long meetingId);

    boolean existsByMeetingIdAndProjectId(Long meetingId, Long projectId);

    void deleteByMeetingIdAndProjectId(Long meetingId, Long projectId);

    @Query("SELECT COALESCE(MAX(mp.orderIndex), -1) + 1 FROM MeetingProject mp WHERE mp.meeting.id = :meetingId")
    Integer getNextOrderIndex(@Param("meetingId") Long meetingId);

    @Query("SELECT COUNT(mp) FROM MeetingProject mp WHERE mp.meeting.id = :meetingId")
    long countByMeetingId(@Param("meetingId") Long meetingId);
}