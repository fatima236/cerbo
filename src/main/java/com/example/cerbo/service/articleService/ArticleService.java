package com.example.cerbo.service.articleService;

import com.example.cerbo.entity.Article;

public interface ArticleService {
    void deleteArticleById(Long id);
    Article findArticleById(Long id);
}
