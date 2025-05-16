package com.example.cerbo.service;

import com.example.cerbo.entity.ResourceDocument;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.ResourceDocumentRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ResourceDocumentService {

    private final ResourceDocumentRepository resourceDocumentRepository;
    private final Path fileStorageLocation;

    public ResourceDocumentService(ResourceDocumentRepository resourceDocumentRepository) {
        this.resourceDocumentRepository = resourceDocumentRepository;
        this.fileStorageLocation = Paths.get("uploads/resources").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public List<ResourceDocument> getAllResourceDocuments() {
        return resourceDocumentRepository.findAll();
    }

    public ResourceDocument getResourceDocumentById(Long id) {
        return resourceDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id " + id));
    }

    public ResourceDocument storeResourceDocument(MultipartFile file, String description, String category) {
        try {
            // Générer un nom unique pour le fichier
            String originalFileName = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            ResourceDocument document = new ResourceDocument();
            document.setName(originalFileName);
            document.setPath(fileName);
            document.setContentType(file.getContentType());
            document.setSize(file.getSize());
            document.setDescription(description);
            document.setCategory(category);
            document.setCreationDate(LocalDateTime.now());

            return resourceDocumentRepository.save(document);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename(), ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found " + fileName, ex);
        }
    }

    public void deleteResourceDocument(Long id) {
        ResourceDocument document = getResourceDocumentById(id);
        try {
            Path filePath = this.fileStorageLocation.resolve(document.getPath()).normalize();
            Files.deleteIfExists(filePath);
            resourceDocumentRepository.deleteById(id);
        } catch (IOException ex) {
            throw new RuntimeException("Error deleting file", ex);
        }
    }

    public List<ResourceDocument> getDocumentsByCategory(String category) {
        return resourceDocumentRepository.findByCategory(category);
    }

    public List<ResourceDocument> searchDocuments(String keyword) {
        return resourceDocumentRepository.findByNameContainingIgnoreCase(keyword);
    }
}