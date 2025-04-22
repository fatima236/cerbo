package com.example.cerbo.service;

import com.example.cerbo.dto.UpdateProfileRequest;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public User getUserProfile(String email) {
        return userRepository.findByEmail(email);
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
            user.setPhotoUrl(fileName); // Ne pas ajouter /uploads/ ici car déjà inclus dans storeFile
        }

        return userRepository.save(user);
    }
}