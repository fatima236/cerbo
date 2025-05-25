package com.example.cerbo.repository;

import com.example.cerbo.entity.ResourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceDocumentRepository extends JpaRepository<ResourceDocument, Long> {

    // Pour investigateurs - documents publics seulement
    List<ResourceDocument> findByIsPublicTrueOrderByCreationDateDesc();

    List<ResourceDocument> findByCategoryAndIsPublicTrueOrderByCreationDateDesc(String category);

    // Pour admin - tous les documents
    List<ResourceDocument> findAllByOrderByCreationDateDesc();

    // Recherche avec filtres pour admin
    @Query("SELECT rd FROM ResourceDocument rd WHERE " +
            "(:category IS NULL OR rd.category = :category) AND " +
            "(:isPublic IS NULL OR rd.isPublic = :isPublic) AND " +
            "(:searchTerm IS NULL OR " +
            "LOWER(rd.originalFileName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(rd.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY rd.creationDate DESC")
    List<ResourceDocument> findWithFilters(@Param("category") String category,
                                           @Param("isPublic") Boolean isPublic,
                                           @Param("searchTerm") String searchTerm);

    // Recherche pour investigateurs (publics seulement)
    @Query("SELECT rd FROM ResourceDocument rd WHERE " +
            "rd.isPublic = true AND " +
            "(:category IS NULL OR rd.category = :category) AND " +
            "(:searchTerm IS NULL OR " +
            "LOWER(rd.originalFileName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(rd.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY rd.creationDate DESC")
    List<ResourceDocument> findPublicWithFilters(@Param("category") String category,
                                                 @Param("searchTerm") String searchTerm);
}