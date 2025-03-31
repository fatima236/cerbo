package com.example.cerbo.controller;

import com.example.cerbo.entity.Article;
import com.example.cerbo.entity.Training;
import com.example.cerbo.repository.ArticleRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/articles")
@CrossOrigin(origins = "http://localhost:3000")
@AllArgsConstructor
public class ArticleController {

    ArticleRepository articleRepository;

    @GetMapping
    public ResponseEntity<List<Article>> getAllArticles() {
        List<Article> articles = articleRepository.findAll();
        articles.forEach(article -> {
            article.getDocuments().forEach(document -> {
                document.setArticle(null);
            });
        });
        return ResponseEntity.ok(articles);
    }

    @PostMapping("/addArticle")
    public ResponseEntity<Article> addEvent(@Valid @RequestBody Article article) {
        return ResponseEntity.ok(articleRepository.save(article));
    }

}
