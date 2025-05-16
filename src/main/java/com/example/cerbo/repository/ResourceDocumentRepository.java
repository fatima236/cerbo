package com.example.cerbo.repository;

import com.example.cerbo.entity.ResourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceDocumentRepository extends JpaRepository<ResourceDocument, Long> {
    List<ResourceDocument> findByCategory(String category);
    List<ResourceDocument> findByNameContainingIgnoreCase(String name);
}