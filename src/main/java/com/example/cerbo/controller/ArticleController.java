package com.example.cerbo.controller;

import com.example.cerbo.entity.Article;
import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Event;
import com.example.cerbo.entity.Training;
import com.example.cerbo.repository.ArticleRepository;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.service.FileStorageService;
import com.example.cerbo.service.articleService.ArticleService;
import com.example.cerbo.service.documentService.DocumentService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/articles")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@AllArgsConstructor
public class ArticleController {

    private final FileStorageService fileStorageService;
    ArticleRepository articleRepository;
    ArticleService articleService;
    DocumentService documentService;
    DocumentRepository documentRepository;



    @GetMapping
    public ResponseEntity<List<Article>> getAllArticles() {
        List<Article> articles = articleRepository.findAll();

        return ResponseEntity.ok(articles);
    }

    @GetMapping("/lastArticles")
    public ResponseEntity<List<Article>> lastArticles() {
        List<Article> articles = articleRepository.findTop4ByOrderByPublicationDateDesc();

        return ResponseEntity.ok(articles);
    }

    @GetMapping("/article/{articleId}")
    public ResponseEntity<Article> getArticleById(@PathVariable Long articleId) {

        Article article = articleRepository.findById(articleId).orElse(null);


        return ResponseEntity.ok(article);
    }

    @PostMapping("/addArticle")
    public ResponseEntity<Article> addArticle(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("summary") String summary,
            @RequestParam("publicationDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime publicationDate,
            @RequestParam("category") String category,
            @RequestParam("text") String text,
            @RequestParam("author") String author,
            @RequestParam("file") MultipartFile file){

    Article savedArticle = articleService.addArticle(title,content,summary,publicationDate,category,text,author,file);

        return ResponseEntity.ok(savedArticle);
    }

    @DeleteMapping("/deleteArticle/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable("id") Long articleId) {
        Article article = articleRepository.findById(articleId).orElse(null);

        if (article == null) {
            return ResponseEntity.notFound().build();
        }
        fileStorageService.deleteFile(article.getFilename());
        articleService.deleteArticleById(articleId);

        return ResponseEntity.noContent().build();
    }


    @PutMapping("/updateArticle/{articleId}")
    public ResponseEntity<Article> updateArticle(@PathVariable("articleId") Long articleId,
                                                @RequestParam("title") String title,
                                                @RequestParam("content") String content,
                                                @RequestParam("summary") String summary,
                                                @RequestParam("publicationDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime publicationDate,
                                                @RequestParam("category") String category,
                                                @RequestParam("text") String text,
                                                @RequestParam("author") String author,
                                                @RequestParam(value = "image", required = false) MultipartFile image) {

        Optional<Article> existingArticleOpt = articleRepository.findById(articleId);

        if (existingArticleOpt.isPresent()) {
            Article existingArticle = existingArticleOpt.get();

            existingArticle.setTitle(title);
            existingArticle.setContent(content);
            existingArticle.setSummary(summary);
            existingArticle.setPublicationDate(publicationDate);
            existingArticle.setCategory(category);
            existingArticle.setText(text);
            existingArticle.setAuthor(author);

            if (image != null && !image.isEmpty()) {
                String fileName = fileStorageService.storeFile(image);
                fileStorageService.deleteFile(existingArticle.getFilename());
                existingArticle.setFilename(fileName);
            }

            Article updatedArticle = articleRepository.save(existingArticle);
            return ResponseEntity.ok(updatedArticle);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
