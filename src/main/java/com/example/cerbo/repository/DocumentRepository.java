package com.example.cerbo.repository;

import com.example.cerbo.entity.Article;
import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Event;
import com.example.cerbo.entity.enums.RemarkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document,Long> {
    List<Document> findDocumentsByTrainingId(Long trainingId);
    List<Document> findDocumentsByArticleId(Long articleId);
    List<Document> findDocumentsByProjectId(Long projectId);
    List<Document> findDocumentsByEventId(Long eventId);
    List<Document> findByProjectId(Long projectId);

    // Ajoutez ces nouvelles méthodes
    List<Document> findByProjectIdAndReviewRemarkIsNotNull(Long projectId);
    List<Document> findByReviewStatusAndReviewRemarkIsNotNull(RemarkStatus status);
    List<Document> findByProjectIdAndAdminStatus(Long projectId, RemarkStatus adminStatus);

    Document getFirstByEvent(Event event);

    Document getFirsByArticle(Article article);

}
