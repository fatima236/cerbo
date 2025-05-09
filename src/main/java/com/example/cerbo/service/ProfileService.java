package com.example.cerbo.service;

import com.example.cerbo.dto.UpdateProfileRequest;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getUserProfile(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public List<User> getAllProfiles() {
        return userRepository.findAll();
    }

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
            user.setPhotoUrl(fileName);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("Utilisateur non trouvé");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Le nouveau mot de passe doit contenir au moins 6 caractères");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}