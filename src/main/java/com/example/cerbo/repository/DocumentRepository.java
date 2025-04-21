package com.example.cerbo.repository;

import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document,Long> {
    List<Document> findDocumentsByTrainingId(Long trainingId);
    List<Document> findDocumentsByArticleId(Long articleId);
    List<Document> findDocumentsByProjectId(Long projectId);
    List<Document> findDocumentsByEventId(Long eventId);

    Document getFirstByEvent(Event event);
}
