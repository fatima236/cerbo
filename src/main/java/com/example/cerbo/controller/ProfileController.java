package com.example.cerbo.controller;

import com.example.cerbo.dto.UpdateProfileRequest;
import com.example.cerbo.entity.User;
import com.example.cerbo.service.ProfileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final ObjectMapper objectMapper; // Add this line

    public ProfileController(ProfileService profileService, ObjectMapper objectMapper) {
        this.profileService = profileService;
        this.objectMapper = objectMapper; // Initialize via constructor injection
    }

    @GetMapping
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = profileService.getUserProfile(email);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Utilisateur introuvable");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("civilite", user.getCivilite() != null ? user.getCivilite() : "");
            response.put("nom", user.getNom() != null ? user.getNom() : "");
            response.put("prenom", user.getPrenom() != null ? user.getPrenom() : "");
            response.put("email", user.getEmail() != null ? user.getEmail() : "");
            response.put("titre", user.getTitre() != null ? user.getTitre() : "");
            response.put("laboratoire", user.getLaboratoire() != null ? user.getLaboratoire() : "");
            response.put("affiliation", user.getAffiliation() != null ? user.getAffiliation() : "");
            response.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl() : "");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de la récupération du profil: " + e.getMessage());
        }
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateProfile(
            @RequestPart(value = "formData") String formDataStr,
            @RequestPart(value = "photoFile", required = false) MultipartFile photoFile,
            Authentication authentication) {

        try {
            UpdateProfileRequest request = objectMapper.readValue(formDataStr, UpdateProfileRequest.class);

            // Validation
            if (StringUtils.isEmpty(request.getCivilite()) ||
                    StringUtils.isEmpty(request.getNom()) ||
                    StringUtils.isEmpty(request.getPrenom())) {
                return ResponseEntity.badRequest()
                        .body("Civilité, nom et prénom sont obligatoires");
            }

            User updatedUser = profileService.updateUserProfile(authentication.getName(), request, photoFile);

            // Retourner l'URL complète de la photo
            String fullPhotoUrl = updatedUser.getPhotoUrl() != null ?
                    "/uploads/" + updatedUser.getPhotoUrl() : null;

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Profil mis à jour",
                    "user", updatedUser,
                    "photoUrl", fullPhotoUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Erreur serveur: " + e.getMessage());
        }
    }
    // Dans ProfileController.java

    @GetMapping("/all")
    public ResponseEntity<?> getAllProfiles() {
        try {
            List<User> users = profileService.getAllProfiles();

            // Simplifier la réponse pour le frontend
            List<Map<String, Object>> response = users.stream().map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("civilite", user.getCivilite());
                userMap.put("nom", user.getNom());
                userMap.put("prenom", user.getPrenom());
                userMap.put("email", user.getEmail());
                userMap.put("titre", user.getTitre());
                userMap.put("affiliation", user.getAffiliation());
                userMap.put("photoUrl", user.getPhotoUrl());
                return userMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de la récupération des profils: " + e.getMessage());
        }
    }
}