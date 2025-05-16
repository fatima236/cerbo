package com.example.cerbo;

import com.example.cerbo.entity.ResourceDocument;
import com.example.cerbo.repository.ResourceDocumentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final ResourceDocumentRepository resourceDocumentRepository;

    @Value("${cerbo.initial-documents.path}")
    private String initialDocumentsPath;

    @Value("${cerbo.uploads.directory}")
    private String uploadsDirectory;

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
            initializeResourceDocumentsFromDirectory();
        } else {
            logger.info("Resource documents already initialized. Skipping.");
        }
    }

    private void initializeResourceDocumentsFromDirectory() {
        logger.info("Initializing resource documents from: {}", initialDocumentsPath);

        // Vérifier que le dossier source existe
        Path sourceDirPath = Paths.get(initialDocumentsPath);
        if (!Files.exists(sourceDirPath)) {
            logger.error("Source directory does not exist: {}", initialDocumentsPath);
            throw new RuntimeException("Source directory not found: " + initialDocumentsPath);
        }

        try {
            // Lister tous les fichiers du répertoire source
            List<ResourceDocument> documents = new ArrayList<>();

            // Parcourir le répertoire et ajouter tous les fichiers docx et pdf
            Files.list(sourceDirPath)
                    .filter(path -> {
                        String filename = path.getFileName().toString().toLowerCase();
                        return filename.endsWith(".docx") || filename.endsWith(".pdf");
                    })
                    .forEach(path -> {
                        try {
                            String filename = path.getFileName().toString();

                            // Créer une entité ResourceDocument pour chaque fichier
                            ResourceDocument document = new ResourceDocument();
                            document.setName(filename);

                            // Générer un nom de fichier unique pour le stockage
                            String storageName = System.currentTimeMillis() + "_" + filename.replaceAll("\\s+", "_");
                            document.setPath(storageName);

                            // Déterminer le type de contenu en fonction de l'extension
                            String contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"; // Par défaut pour .docx
                            if (filename.toLowerCase().endsWith(".pdf")) {
                                contentType = "application/pdf";
                            }
                            document.setContentType(contentType);

                            // Obtenir la taille du fichier
                            document.setSize(Files.size(path));

                            // Générer une catégorie en fonction du nom du fichier
                            String category = determineCategory(filename);
                            document.setCategory(category);

                            // Date de création = maintenant
                            document.setCreationDate(LocalDateTime.now());

                            // Générer une description basée sur le nom du fichier
                            document.setDescription(generateDescription(filename));

                            // Copier le fichier source vers le dossier des ressources
                            Path targetPath = resourceFilesPath.resolve(storageName);
                            Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);

                            // Ajouter le document à la liste
                            documents.add(document);

                            logger.info("Processed document: {}", filename);

                        } catch (Exception e) {
                            logger.error("Error processing file {}: {}", path.getFileName(), e.getMessage(), e);
                        }
                    });

            // Sauvegarder tous les documents préparés
            if (!documents.isEmpty()) {
                resourceDocumentRepository.saveAll(documents);
                logger.info("Saved {} resource documents", documents.size());
            } else {
                logger.warn("No documents were processed successfully");
            }

        } catch (IOException e) {
            logger.error("Error scanning directory: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to scan directory", e);
        }
    }

    private String determineCategory(String fileName) {
        fileName = fileName.toLowerCase();

        if (fileName.contains("model") || fileName.contains("modèle") || fileName.contains("modele")) {
            return "Modèles";
        } else if (fileName.contains("fiche") || fileName.contains("information")) {
            return "Fiches d'information";
        } else if (fileName.contains("consent")) {
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
        // Retirer l'extension
        String nameWithoutExt = fileName.replaceAll("\\.docx$|\\.pdf$", "");

        // Remplacer les underscores par des espaces
        nameWithoutExt = nameWithoutExt.replace('_', ' ');

        // Générer une description personnalisée selon le type de document
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
        } else {
            // Description générique
            return "Document ressource officiel: " + nameWithoutExt;
        }
    }
}