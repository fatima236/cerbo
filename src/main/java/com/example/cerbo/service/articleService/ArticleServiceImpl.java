package com.example.cerbo.service.articleService;

import com.example.cerbo.entity.Article;
import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Event;
import com.example.cerbo.repository.ArticleRepository;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.service.documentService.DocumentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleServiceImpl implements ArticleService {
    private final ArticleRepository articleRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;

    public ArticleServiceImpl(ArticleRepository articleRepository, DocumentRepository documentRepository, DocumentService documentService) {
        this.articleRepository = articleRepository;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
    }

    @Override
    public void deleteArticleById(Long id) {
        Article article = articleRepository.getById(id);
        List<Document> documents = documentRepository.findDocumentsByEventId(id);
        for(Document document: documents) {
            documentService.removeDocumentById(document.getId());
        }
        articleRepository.delete(article);
    }

    @Override
    public Article findArticleById(Long id) {
        return null;
    }
}
