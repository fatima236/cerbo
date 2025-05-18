package com.example.cerbo;

import com.example.cerbo.entity.ResourceDocument;
import com.example.cerbo.repository.ResourceDocumentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final ResourceDocumentRepository resourceDocumentRepository;

    @Value("${cerbo.uploads.directory}")
    private String uploadsDirectory;

    @Value("${cerbo.initial-documents.path:documents/initial}")
    private String initialDocumentsPath;

    private Path resourceFilesPath;

    @Autowired
    public DataLoader(ResourceDocumentRepository resourceDocumentRepository) {
        this.resourceDocumentRepository = resourceDocumentRepository;
    }

    @PostConstruct
    public void init() {
        this.resourceFilesPath = Paths.get(uploadsDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.resourceFilesPath);
            logger.info("Created upload directory: {}", resourceFilesPath);
        } catch (IOException e) {
            logger.error("Failed to create upload directory", e);
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        // Vérifier si des documents existent déjà
        if (resourceDocumentRepository.count() == 0) {
            initializeResourceDocumentsFromClasspath();
        } else {
            logger.info("Resource documents already initialized. Count: {}", resourceDocumentRepository.count());
        }
    }

    private void initializeResourceDocumentsFromClasspath() {
        logger.info("Initializing resource documents from: {}", initialDocumentsPath);

        // Liste des documents à initialiser
        String[] documentNames = {
                "Rapport_1er examen_Projet_Reference.docx",
                "Message_Declaration_Conflit d'interet et declartion_honneur.docx",
                "Descriptif_projet.docx",
                "Avis final-Projet_modele_VF.docx",
                "Soumission de Projet.docx",
                "Attestation d'engagement_nouvelle version.docx",
                "Message_validation_enregistrement_membre.docx",
                "Modele PV_final_Projet.docx",
                "Modèle PV_Réunion.docx",
                "Modèle CV à remplir.docx",
                "Consideration ethique.docx",
                "Fiche information_ Français.docx",
                "Fiche information_ Arabe.docx",
                "Fiche de consentement Français.docx",
                "Fiche de consentement_ Arabe.docx",
                "Attestation CNDP.docx"
        };

        List<ResourceDocument> documents = new ArrayList<>();
        int processedCount = 0;
        int skippedCount = 0;

        for (String documentName : documentNames) {
            try {
                // Utiliser la propriété de configuration
                String resourcePath = initialDocumentsPath + "/" + documentName;
                Resource resource = new ClassPathResource(resourcePath);

                if (!resource.exists()) {
                    logger.warn("Document not found in classpath: {} - Skipping", resourcePath);
                    skippedCount++;
                    continue;
                }

                // Créer une entité ResourceDocument
                ResourceDocument document = new ResourceDocument();
                document.setName(documentName);

                // Générer un nom de fichier unique pour le stockage
                String storageName = System.currentTimeMillis() + "_" + documentName.replaceAll("\\s+", "_");
                document.setPath(storageName);

                // Déterminer le type de contenu
                String contentType = determineContentType(documentName);
                document.setContentType(contentType);

                // Obtenir la taille du fichier
                document.setSize(resource.contentLength());

                // Générer une catégorie et description
                document.setCategory(determineCategory(documentName));
                document.setDescription(generateDescription(documentName));
                document.setCreationDate(LocalDateTime.now());

                // Copier le fichier des ressources vers le dossier de destination
                Path targetPath = resourceFilesPath.resolve(storageName);
                Files.copy(resource.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                documents.add(document);
                processedCount++;
                logger.info("✓ Processed document: {} ({} bytes)", documentName, document.getSize());

            } catch (Exception e) {
                logger.error("✗ Error processing document {}: {}", documentName, e.getMessage());
                skippedCount++;
            }
        }

        // Sauvegarder tous les documents
        if (!documents.isEmpty()) {
            resourceDocumentRepository.saveAll(documents);
            logger.info("Successfully saved {} resource documents to database", documents.size());
        } else {
            logger.warn("No documents were processed successfully");
        }

        // Résumé final
        logger.info("Initialization complete: {} processed, {} skipped, {} total expected",
                processedCount, skippedCount, documentNames.length);
    }

    private String determineContentType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerName.endsWith(".doc")) {
            return "application/msword";
        } else {
            return "application/octet-stream";
        }
    }

    private String determineCategory(String fileName) {
        fileName = fileName.toLowerCase();

        if (fileName.contains("model") || fileName.contains("modèle") || fileName.contains("modele")) {
            return "Modèles";
        } else if (fileName.contains("fiche") || fileName.contains("information")) {
            return "Fiches d'information";
        } else if (fileName.contains("consent") || fileName.contains("consentement")) {
            return "Formulaires de consentement";
        } else if (fileName.contains("avis") || fileName.contains("rapport")) {
            return "Rapports";
        } else if (fileName.contains("attestation")) {
            return "Attestations";
        } else if (fileName.contains("descriptif") || fileName.contains("consideration")) {
            return "Guides";
        } else if (fileName.contains("pv") || fileName.contains("réunion")) {
            return "Procès-verbaux";
        } else if (fileName.contains("message") || fileName.contains("declaration")) {
            return "Communications officielles";
        } else if (fileName.contains("cv")) {
            return "Formulaires CV";
        } else {
            return "Autres documents";
        }
    }

    private String generateDescription(String fileName) {
        String nameWithoutExt = fileName.replaceAll("\\.docx$|\\.pdf$|\\.doc$", "");
        nameWithoutExt = nameWithoutExt.replace('_', ' ');

        if (nameWithoutExt.toLowerCase().contains("consentement")) {
            return "Formulaire de consentement officiel à utiliser pour les participants à l'étude";
        } else if (nameWithoutExt.toLowerCase().contains("cv")) {
            return "Modèle de CV à compléter par les investigateurs";
        } else if (nameWithoutExt.toLowerCase().contains("fiche information")) {
            return "Fiche d'information à présenter aux participants de l'étude";
        } else if (nameWithoutExt.toLowerCase().contains("consideration ethique")) {
            return "Guide explicatif des considérations éthiques à prendre en compte dans votre projet";
        } else if (nameWithoutExt.toLowerCase().contains("attestation")) {
            return "Document officiel d'attestation à inclure dans votre dossier";
        } else if (nameWithoutExt.toLowerCase().contains("soumission")) {
            return "Guide détaillé pour la soumission de votre projet au CERBO";
        } else if (nameWithoutExt.toLowerCase().contains("rapport")) {
            return "Modèle de rapport d'examen pour l'évaluation de projet";
        } else if (nameWithoutExt.toLowerCase().contains("message")) {
            return "Message type pour communication avec les participants ou membres";
        } else if (nameWithoutExt.toLowerCase().contains("avis")) {
            return "Modèle d'avis final pour l'évaluation de projet";
        } else if (nameWithoutExt.toLowerCase().contains("pv")) {
            return "Modèle de procès-verbal pour les réunions du comité";
        } else if (nameWithoutExt.toLowerCase().contains("descriptif")) {
            return "Guide pour la description de projet de recherche";
        } else {
            return "Document ressource officiel: " + nameWithoutExt;
        }
    }
}