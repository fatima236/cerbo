package com.example.cerbo.service;

import com.example.cerbo.entity.ResourceDocument;
import com.example.cerbo.entity.User;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.ResourceDocumentRepository;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ResourceDocumentService {

    private final ResourceDocumentRepository resourceDocumentRepository;
    private final UserRepository userRepository;

    private final Path fileStorageLocation = Paths.get("uploads/resources").toAbsolutePath().normalize();

    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create upload directory", ex);
        }
    }

    // Créer un document (ADMIN)
    public ResourceDocument createResourceDocument(MultipartFile file, String description,
                                                   String category, String tags,
                                                   Boolean isPublic, String adminEmail) {
        init();
        try {
            String originalFileName = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            User admin = userRepository.findByEmail(adminEmail);

            ResourceDocument document = new ResourceDocument();
            document.setName(fileName);
            document.setOriginalFileName(originalFileName);
            document.setPath(fileName);
            document.setContentType(file.getContentType());
            document.setSize(file.getSize());
            document.setDescription(description != null ? description : "");
            document.setCategory(category);
            document.setTags(tags != null ? tags : "");
            document.setIsPublic(isPublic != null ? isPublic : true); // Par défaut public
            document.setCreatedBy(admin);
            document.setCreationDate(LocalDateTime.now());

            return resourceDocumentRepository.save(document);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file", ex);
        }
    }

    // Mettre à jour un document (ADMIN)
    public ResourceDocument updateResourceDocument(Long id, String description,
                                                   String category, String tags,
                                                   Boolean isPublic, String adminEmail) {
        ResourceDocument document = getResourceDocumentById(id);
        User admin = userRepository.findByEmail(adminEmail);

        if (description != null) document.setDescription(description);
        if (category != null) document.setCategory(category);
        if (tags != null) document.setTags(tags);
        if (isPublic != null) document.setIsPublic(isPublic);

        document.setUpdatedBy(admin);
        document.setModificationDate(LocalDateTime.now());

        return resourceDocumentRepository.save(document);
    }

    // Supprimer un document (ADMIN)
    public void deleteResourceDocument(Long id, String adminEmail) {
        ResourceDocument document = getResourceDocumentById(id);
        try {
            Path filePath = this.fileStorageLocation.resolve(document.getPath()).normalize();
            Files.deleteIfExists(filePath);
            resourceDocumentRepository.deleteById(id);
        } catch (IOException ex) {
            throw new RuntimeException("Error deleting file", ex);
        }
    }

    // Basculer la visibilité public/privé (ADMIN)
    public ResourceDocument toggleVisibility(Long id, String adminEmail) {
        ResourceDocument document = getResourceDocumentById(id);
        User admin = userRepository.findByEmail(adminEmail);

        document.setIsPublic(!document.getIsPublic());
        document.setUpdatedBy(admin);
        document.setModificationDate(LocalDateTime.now());

        return resourceDocumentRepository.save(document);
    }

    // Lister les documents publics (INVESTIGATEURS)
    public List<ResourceDocument> getPublicResourceDocuments() {
        return resourceDocumentRepository.findByIsPublicTrueOrderByCreationDateDesc();
    }

    // Lister par catégorie (INVESTIGATEURS)
    public List<ResourceDocument> getPublicResourceDocumentsByCategory(String category) {
        return resourceDocumentRepository.findByCategoryAndIsPublicTrueOrderByCreationDateDesc(category);
    }

    // Recherche pour investigateurs
    public List<ResourceDocument> searchPublicResourceDocuments(String category, String searchTerm) {
        return resourceDocumentRepository.findPublicWithFilters(category, searchTerm);
    }

    // Lister tous les documents (ADMIN)
    public List<ResourceDocument> getAllResourceDocuments() {
        return resourceDocumentRepository.findAllByOrderByCreationDateDesc();
    }

    // Recherche avec filtres (ADMIN)
    public List<ResourceDocument> searchResourceDocuments(String category, Boolean isPublic, String searchTerm) {
        return resourceDocumentRepository.findWithFilters(category, isPublic, searchTerm);
    }

    // Télécharger un document
    @Transactional
    public Resource downloadResourceDocument(Long id, boolean isAdmin) {
        ResourceDocument document = getResourceDocumentById(id);

        // Vérifier les permissions : investigateurs ne peuvent accéder qu'aux documents publics
        if (!isAdmin && !document.getIsPublic()) {
            throw new ResourceNotFoundException("Document not accessible");
        }

        // Incrémenter le compteur
        document.incrementDownloadCount();
        resourceDocumentRepository.save(document);

        return loadFileAsResource(document.getPath());
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

    public ResourceDocument getResourceDocumentById(Long id) {
        return resourceDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id " + id));
    }

    public List<Map<String, String>> getAvailableCategories() {
        return Arrays.asList(
                Map.of("value", "FORMULAIRES", "label", "Formulaires"),
                Map.of("value", "GUIDES", "label", "Guides"),
                Map.of("value", "MODELES", "label", "Modèles"),
                Map.of("value", "PROCEDURES", "label", "Procédures"),
                Map.of("value", "AUTRES", "label", "Autres")
        );
    }

    // Convertir en Map pour les réponses
    public Map<String, Object> convertToMap(ResourceDocument doc, boolean isAdmin) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", doc.getId());
        map.put("name", doc.getOriginalFileName());
        map.put("description", doc.getDescription());
        map.put("category", doc.getCategory());
        map.put("size", doc.getFormattedSize());
        map.put("contentType", doc.getContentType());
        map.put("downloadCount", doc.getDownloadCount());
        map.put("creationDate", doc.getCreationDate());
        map.put("isPublic", doc.getIsPublic()); // Toujours inclus

        if (isAdmin) {
            // Informations supplémentaires pour admin
            map.put("tags", doc.getTags());
            map.put("modificationDate", doc.getModificationDate());

            if (doc.getCreatedBy() != null) {
                map.put("createdBy", doc.getCreatedBy().getPrenom() + " " + doc.getCreatedBy().getNom());
            }
        }

        return map;
    }
}