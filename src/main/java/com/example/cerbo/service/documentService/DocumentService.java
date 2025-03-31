package com.example.cerbo.service.documentService;

import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Event;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface DocumentService {

    public Document getDocumentById(Long id);
    public String uploadFile(MultipartFile file, Long eventId, Long articleId, Long projectId, Long trainingId);
    public List<Document> documentsOfTraining(Long trainingId);
    public List<Document> documentsOfArticle(Long articleId);
    public List<Document> documentsOfProject(Long projectId);
    public List<Document> documentsOfEvent(Long eventId);
    public String  removeDocumentById(Long id);


}
