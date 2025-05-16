package com.example.cerbo.controller;

import com.example.cerbo.entity.ResourceDocument;
import com.example.cerbo.service.ResourceDocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resource-documents")
@CrossOrigin(origins = "http://localhost:3000")
public class ResourceDocumentController {

    private final ResourceDocumentService resourceDocumentService;

    public ResourceDocumentController(ResourceDocumentService resourceDocumentService) {
        this.resourceDocumentService = resourceDocumentService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllResourceDocuments() {
        List<ResourceDocument> documents = resourceDocumentService.getAllResourceDocuments();

        List<Map<String, Object>> response = documents.stream()
                .map(doc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", doc.getId());
                    map.put("name", doc.getName());
                    map.put("type", doc.getContentType());
                    map.put("size", formatFileSize(doc.getSize()));
                    map.put("date", doc.getCreationDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    map.put("category", doc.getCategory() != null ? doc.getCategory() : "Autres");
                    map.put("description", doc.getDescription() != null ? doc.getDescription() : "");
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Map<String, Object>>> getDocumentsByCategory(@PathVariable String category) {
        List<ResourceDocument> documents = resourceDocumentService.getDocumentsByCategory(category);

        List<Map<String, Object>> response = documents.stream()
                .map(doc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", doc.getId());
                    map.put("name", doc.getName());
                    map.put("type", doc.getContentType());
                    map.put("size", formatFileSize(doc.getSize()));
                    map.put("date", doc.getCreationDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    map.put("category", doc.getCategory());
                    map.put("description", doc.getDescription() != null ? doc.getDescription() : "");
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        ResourceDocument document = resourceDocumentService.getResourceDocumentById(id);
        Resource resource = resourceDocumentService.loadFileAsResource(document.getPath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getName() + "\"")
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .body(resource);
    }

    @PostMapping
    public ResponseEntity<ResourceDocument> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("description") String description,
            @RequestParam(value = "category", required = false) String category) {

        ResourceDocument document = resourceDocumentService.storeResourceDocument(file, description, category);
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        resourceDocumentService.deleteResourceDocument(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchDocuments(@RequestParam String keyword) {
        List<ResourceDocument> documents = resourceDocumentService.searchDocuments(keyword);

        List<Map<String, Object>> response = documents.stream()
                .map(doc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", doc.getId());
                    map.put("name", doc.getName());
                    map.put("type", doc.getContentType());
                    map.put("size", formatFileSize(doc.getSize()));
                    map.put("date", doc.getCreationDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    map.put("category", doc.getCategory() != null ? doc.getCategory() : "Autres");
                    map.put("description", doc.getDescription() != null ? doc.getDescription() : "");
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    private String formatFileSize(Long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double)size / (1L << (z*10)), " KMGTPE".charAt(z));
    }
}