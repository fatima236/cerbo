package com.example.cerbo.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    // Solution 1: Injection via constructeur (recommandée)
    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        try {
            this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    // Solution alternative 2: Utilisation de @PostConstruct
    /*
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        try {
            this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException(
                "Could not create the directory where the uploaded files will be stored.", ex);
        }
    }
    */

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        try {
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());

            if (fileName.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence: " + fileName);
            }

            String extension = fileName.substring(fileName.lastIndexOf('.'));
            String uniqueFileName = UUID.randomUUID() + extension;

            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return uniqueFileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    public byte[] loadFileAsBytes(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + filename, e);
        }
    }

    public void deleteFile(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + filename, e);
        }
    }
}