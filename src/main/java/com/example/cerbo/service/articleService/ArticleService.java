package com.example.cerbo.service.articleService;

import com.example.cerbo.entity.Article;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public interface ArticleService {
    void deleteArticleById(Long id);
    Article findArticleById(Long id);
    Article addArticle(String title
            , String content
            , String summary
            , LocalDateTime publicationDate
            , String category
            , String text
            ,String author
            ,MultipartFile file);
}
