package com.example.cerbo.repository;

import com.example.cerbo.entity.DocumentReview;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentReviewRepository extends JpaRepository<DocumentReview, Long> {
    List<DocumentReview> findByDocumentId(Long documentId);
    List<DocumentReview> findByDocumentProjectIdAndReviewer(Long projectId, User reviewer);

    long countByDocumentProjectIdAndReviewer(Long projectId, User reviewer);

    List<DocumentReview> findByDocumentProjectIdAndFinalizedTrue(Long projectId);

    List<DocumentReview> findByDocumentProjectId(Long projectId);

    @Query("SELECT dr FROM DocumentReview dr " +
            "WHERE dr.document.project.id = :projectId " +
            "AND dr.remark IS NOT NULL " +
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
}
