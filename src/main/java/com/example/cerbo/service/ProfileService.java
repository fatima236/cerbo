package com.example.cerbo.service;

import com.example.cerbo.dto.UpdateProfileRequest;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.example.cerbo.annotation.Loggable;
@Service
@RequiredArgsConstructor
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;


    @Transactional(readOnly = true)
    public User getUserProfile(String email) {
        return userRepository.findByEmail(email);
    }
// Dans ProfileService.java


    public List<User> getAllProfiles() {
        return userRepository.findAll(); // Supposant que vous utilisez JPA
    }

    @Loggable(actionType = "UPDATE", entityType = "USER")
    @Transactional
    public User updateUserProfile(String email, UpdateProfileRequest request, MultipartFile photoFile) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("Utilisateur non trouvé");
        }

        user.setCivilite(request.getCivilite());
        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setTitre(request.getTitre());
        user.setLaboratoire(request.getLaboratoire());
        user.setAffiliation(request.getAffiliation());

        if (photoFile != null && !photoFile.isEmpty()) {
            String fileName = fileStorageService.storeFile(photoFile);
            user.setPhotoUrl(fileName); // Ne pas ajouter /uploads/ ici car déjà inclus dans storeFile
        }

        return userRepository.save(user);
    }
    @Autowired
    private PasswordEncoder passwordEncoder;
    // Dans ProfileService.java
    @Loggable(actionType = "UPDATE", entityType = "USER")
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("Utilisateur non trouvé");
        }

        // Vérifiez que le mot de passe actuel est correct
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }

        // Validez le nouveau mot de passe
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Le nouveau mot de passe doit contenir au moins 6 caractères");
        }

        // Encodez et enregistrez le nouveau mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}