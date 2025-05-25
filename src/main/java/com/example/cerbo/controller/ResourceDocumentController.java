package com.example.cerbo.controller;

import com.example.cerbo.entity.ResourceDocument;
import com.example.cerbo.service.ResourceDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resource-documents")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class ResourceDocumentController {

    private final ResourceDocumentService resourceDocumentService;

    // =============== ENDPOINTS COMMUNS ===============

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getResourceDocuments(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            Authentication authentication) {

        boolean isAdmin = isUserAdmin(authentication);
        List<ResourceDocument> documents;

        if (isAdmin) {
            // Admin voit tous les documents
            documents = resourceDocumentService.searchResourceDocuments(category, null, search);
        } else {
            // Investigateurs voient seulement les documents publics
            documents = resourceDocumentService.searchPublicResourceDocuments(category, search);
        }

        List<Map<String, Object>> response = documents.stream()
                .map(doc -> resourceDocumentService.convertToMap(doc, isAdmin))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, Authentication authentication) {
        try {
            boolean isAdmin = isUserAdmin(authentication);
            ResourceDocument document = resourceDocumentService.getResourceDocumentById(id);
            Resource resource = resourceDocumentService.downloadResourceDocument(id, isAdmin);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                    .contentType(MediaType.parseMediaType(document.getContentType()))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, String>>> getCategories() {
        return ResponseEntity.ok(resourceDocumentService.getAvailableCategories());
    }

    // =============== ENDPOINTS ADMIN SEULEMENT ===============

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createResourceDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "isPublic", required = false) Boolean isPublic,
            Authentication authentication) {

        try {
            ResourceDocument document = resourceDocumentService.createResourceDocument(
                    file, description, category, tags, isPublic, authentication.getName());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Document ajouté avec succès",
                    "document", resourceDocumentService.convertToMap(document, true)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateResourceDocument(
            @PathVariable Long id,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "isPublic", required = false) Boolean isPublic,
            Authentication authentication) {

        try {
            ResourceDocument document = resourceDocumentService.updateResourceDocument(
                    id, description, category, tags, isPublic, authentication.getName());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Document mis à jour avec succès",
                    "document", resourceDocumentService.convertToMap(document, true)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteResourceDocument(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            resourceDocumentService.deleteResourceDocument(id, authentication.getName());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Document supprimé avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}/toggle-visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> toggleDocumentVisibility(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            ResourceDocument document = resourceDocumentService.toggleVisibility(id, authentication.getName());
            String visibility = document.getIsPublic() ? "public" : "privé";

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Document rendu " + visibility + " avec succès",
                    "document", resourceDocumentService.convertToMap(document, true)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllDocumentsForAdmin(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) String search) {

        List<ResourceDocument> documents = resourceDocumentService.searchResourceDocuments(category, isPublic, search);

        List<Map<String, Object>> response = documents.stream()
                .map(doc -> resourceDocumentService.convertToMap(doc, true))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // =============== MÉTHODE UTILITAIRE ===============

    private boolean isUserAdmin(Authentication authentication) {
        return authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}