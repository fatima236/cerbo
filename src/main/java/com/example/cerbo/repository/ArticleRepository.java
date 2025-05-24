package com.example.cerbo.repository;

import com.example.cerbo.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article,Long> {
    List<Article> findTop4ByOrderByPublicationDateDesc();
}
