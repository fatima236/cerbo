package com.example.cerbo.repository;

import com.example.cerbo.entity.DocumentReview;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.DocumentType;
import com.example.cerbo.entity.enums.RemarkStatus;
import jakarta.persistence.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentReviewRepository extends JpaRepository<DocumentReview, Long> {
    List<DocumentReview> findByDocumentId(Long documentId);
    List<DocumentReview> findByDocumentProjectIdAndReviewer(Long projectId, User reviewer);

    long countByDocumentProjectIdAndReviewer(Long projectId, User reviewer);

    List<DocumentReview> findByProjectIdAndFinalizedTrue(Long projectId);

    List<DocumentReview> findByProjectIdAndFinalizedTrueAndIncludedInReportFalse(Long projectId);

    List<DocumentReview> findByDocumentProjectId(Long projectId);

    @Query("SELECT dr FROM DocumentReview dr " +
            "WHERE dr.document.project.id = :projectId " +
            "AND dr.content IS NOT NULL " +
            "AND dr.status = 'VALIDATED'")
    List<DocumentReview> findValidatedRemarksByProjectId(@Param("projectId") Long projectId);

    List<DocumentReview> findByDocumentIdIn(List<Long> documentIds);
    // @Query("SELECT COUNT(dr) FROM DocumentReview dr WHERE dr.document.project.id = :projectId AND dr.reviewer = :reviewer")
    // long countByProjectAndReviewer(@Param("projectId") Long projectId, @Param("reviewer") User reviewer);

    @Query("SELECT DISTINCT d.type FROM DocumentReview dr " +
            "JOIN dr.document d " +
            "WHERE d.project.id = :projectId " +
            "AND dr.status = 'VALIDATED'")
    List<DocumentType> findDocumentTypeByProjectId(@Param("projectId") Long projectId);

    List<DocumentReview> findByProjectIdAndStatus(Long projectId, RemarkStatus status);

    @Query("SELECT dr FROM DocumentReview dr WHERE dr.project.id = :projectId AND dr.status = :status AND dr.includedInReport = false AND TRIM(dr.content) <> '' AND dr.content is not null ")
    List<DocumentReview> findValidatedUnreportedRemarks(@Param("projectId") Long projectId, @Param("status") RemarkStatus status);

    List<DocumentReview> findByReportIdAndIncludedInReportTrue(Long reportId);

    Optional<DocumentReview> findByDocumentIdAndReviewerId(Long documentId, Long reviewerId);

    @Query("SELECT dr.id FROM DocumentReview dr " +
            "WHERE dr.project.id = :projectId " +
            "AND dr.status = 'VALIDATED' " +
            "AND dr.content <> ''")
    List<Long> documentReviewValidated(@Param("projectId") Long projectId);

    @Query("SELECT dr FROM DocumentReview dr " +
            "JOIN FETCH dr.document d " +
            "JOIN FETCH dr.reviewer r " +
            "WHERE dr.project.id = :projectId " +
            "AND dr.status = 'VALIDATED'")
    List<DocumentReview> findValidatedRemarksWithDocuments(@Param("projectId") Long projectId);

    // Ajoutez cette méthode
    @Query("SELECT dr FROM DocumentReview dr " +
            "WHERE dr.project.id = :projectId " +
            "AND dr.finalized = true " +
            "AND dr.includedInReport = true")
    List<DocumentReview> findFinalRemarksForReport(@Param("projectId") Long projectId);



    @Query("SELECT dr FROM DocumentReview dr WHERE dr.report.id = :reportId")
    List<DocumentReview> findByReportId(@Param("reportId") Long reportId);

    @Query("SELECT DISTINCT dr.reviewer FROM DocumentReview dr " +
            "WHERE dr.final_submission = true " +
            "AND dr.project.id IN (" +
            "    SELECT mp.project.id FROM MeetingProject mp WHERE mp.meeting.id = :meetingId" +
            ")")
    List<User> findFinalExaminersByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT COUNT(dr) FROM DocumentReview dr " +
            "WHERE dr.reviewer = :reviewer " +
            "AND dr.final_submission = :finalSubmission " +
            "AND dr.project.id IN (" +
            "    SELECT mp.project.id FROM MeetingProject mp WHERE mp.meeting.id = :meetingId" +
            ")")
    Long countByReviewerAndFinalSubmissionAndProjectMeetingId(
            @Param("reviewer") User reviewer,
            @Param("finalSubmission") Boolean finalSubmission,
            @Param("meetingId") Long meetingId);
}