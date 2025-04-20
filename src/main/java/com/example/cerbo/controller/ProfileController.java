package com.example.cerbo.controller;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api") // Ajout d'un préfixe d'API
public class ProfileController {

    private final UserRepository userRepository;

    // Injection par constructeur
    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/profile")

    public ResponseEntity<?> createOrUpdateProfile(@RequestBody Map<String, String> payload, Authentication authentication) {
        try {
            System.out.println("[ProfileController] Payload reçu: " + payload);

            String email = authentication.getName(); // 100% fiable avec la sécurité
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("L'email est obligatoire pour retrouver l'utilisateur");
            }
            System.out.println("[ProfileController] Email fourni: " + email);

            User user = userRepository.findByEmail(email);
            if (user == null) {
                System.out.println("[ProfileController] Utilisateur non trouvé pour email: " + email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur introuvable");
            }

            updateUserFields(user, payload);
            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Profil mis à jour avec succès");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    private void updateUserFields(User user, Map<String, String> payload) {
        // Validation et mise à jour des champs
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