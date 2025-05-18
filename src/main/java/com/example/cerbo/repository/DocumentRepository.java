package com.example.cerbo.repository;

import com.example.cerbo.dto.DocumentInfoDTO;
import com.example.cerbo.entity.Article;
import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Event;
import com.example.cerbo.entity.enums.RemarkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document,Long> {
    List<Document> findDocumentsByTrainingId(Long trainingId);
    List<Document> findDocumentsByArticleId(Long articleId);
    List<Document> findDocumentsByProjectId(Long projectId);
    List<Document> findDocumentsByEventId(Long eventId);
    List<Document> findByProjectId(Long projectId);

//    // Ajoutez ces nouvelles méthodes
//    List<Document> findByProjectIdAndReviewRemarkIsNotNull(Long projectId);
//    List<Document> findByReviewStatusAndReviewRemarkIsNotNull(RemarkStatus status);


    Document getFirstByEvent(Event event);

    Document getFirsByArticle(Article article);

//    @Query("SELECT NEW com.example.cerbo.dto.DocumentInfoDTO(d.id, d.name, d.type) " +
//            "FROM Document d WHERE d.project.id = :projectId")
//    List<DocumentInfoDTO> findBasicInfoByProjectId(Long projectId);

}
