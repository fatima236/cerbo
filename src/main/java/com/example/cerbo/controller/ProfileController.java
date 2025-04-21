package com.example.cerbo.controller;

import com.example.cerbo.entity.User;
import com.example.cerbo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur introuvable");
            }

            Map<String, String> profile = new HashMap<>();
            profile.put("civilite", user.getCivilite());
            profile.put("nom", user.getNom());
            profile.put("prenom", user.getPrenom());
            profile.put("email", user.getEmail());
            profile.put("titre", user.getTitre());
            profile.put("laboratoire", user.getLaboratoire());
            profile.put("affiliation", user.getAffiliation());

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Erreur interne du serveur"));
        }
    }



    private void updateUserFields(User user, Map<String, String> payload) {
        if (payload.containsKey("civilite")) {
            user.setCivilite(payload.get("civilite"));
        }

        if (payload.containsKey("nom")) {
            String nom = payload.get("nom");
            if (nom == null || nom.trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom est obligatoire");
            }
            user.setNom(nom);
        }

        if (payload.containsKey("prenom")) {
            String prenom = payload.get("prenom");
            if (prenom == null || prenom.trim().isEmpty()) {
                throw new IllegalArgumentException("Le prénom est obligatoire");
            }
            user.setPrenom(prenom);
        }

        if (payload.containsKey("titre")) {
            user.setTitre(payload.get("titre"));
        }

        if (payload.containsKey("laboratoire")) {
            user.setLaboratoire(payload.get("laboratoire"));
        }

        if (payload.containsKey("affiliation")) {
            user.setAffiliation(payload.get("affiliation"));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        response.put("status", "error");
        return response;
    }
}