package com.example.cerbo.service.articleService;

import com.example.cerbo.entity.Article;
import com.example.cerbo.entity.Document;
import com.example.cerbo.repository.ArticleRepository;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.service.FileStorageService;
import com.example.cerbo.service.documentService.DocumentService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ArticleServiceImpl implements ArticleService {
    private final ArticleRepository articleRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final FileStorageService fileStorageService;

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

    @Override
    public Article addArticle(String title
            , String content
            , String summary
            , LocalDateTime publicationDate
            , String category
            , String text
            , String author
            , MultipartFile file){

        Article article = new Article();

        article.setTitle(title);
        article.setContent(content);
        article.setSummary(summary);
        article.setPublicationDate(publicationDate);
        article.setCategory(category);
        article.setText(text);
        article.setAuthor(author);


        String filename = fileStorageService.storeFile(file);
        article.setFilename(filename);

        return articleRepository.save(article);


    }
}
