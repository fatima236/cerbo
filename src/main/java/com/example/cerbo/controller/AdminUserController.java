package com.example.cerbo.controller;

import com.example.cerbo.annotation.Loggable;
import com.example.cerbo.entity.PendingUser;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.RoleType;
import com.example.cerbo.service.AdminUserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGATEUR', 'EVALUATEUR')")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /**
     * Récupérer tous les utilisateurs
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    /**
     * Récupérer un utilisateur par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return adminUserService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupérer toutes les demandes d'inscription en attente
     */
    @GetMapping("/pending")
    public ResponseEntity<List<PendingUser>> getAllPendingUsers() {
        return ResponseEntity.ok(adminUserService.getAllPendingUsers());
    }

    /**
     * Récupérer une demande d'inscription par ID
     */
    @GetMapping("/pending/{id}")
    public ResponseEntity<?> getPendingUserById(@PathVariable Long id) {
        return adminUserService.getPendingUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Approuver une demande d'inscription en tant qu'investigateur
     */


    // ... autres méthodes inchangées ...

    /**
     * Approuver une demande d'inscription en tant qu'investigateur
     */
    @PostMapping("/pending/{id}/approve-investigator")
    public ResponseEntity<?> approveInvestigator(@PathVariable Long id) {
        try {
            User user = adminUserService.approveInvestigator(id);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Demande approuvée en tant qu'investigateur et email envoyé",
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "civilite", user.getCivilite(),
                            "nom", user.getNom(),
                            "prenom", user.getPrenom(),
                            "photoUrl", user.getPhotoUrl() != null ?
                                    "/api/users/" + user.getId() + "/photo" : null,
                            "roles", user.getRoles()
                    )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "error", "Erreur lors de l'approbation: " + e.getMessage()
            ));
        }
    }

    /**
     * Approuver une demande d'inscription en tant qu'évaluateur
     */
    @PostMapping("/pending/{id}/approve-evaluator")
    public ResponseEntity<?> approveAsEvaluator(@PathVariable Long id) {
        try {
            User user = adminUserService.approveAsEvaluator(id);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Demande approuvée en tant qu'évaluateur et email envoyé",
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "civilite", user.getCivilite(),
                            "nom", user.getNom(),
                            "prenom", user.getPrenom(),
                            "roles", user.getRoles()
                    )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "error", "Erreur lors de l'approbation: " + e.getMessage()
            ));
        }
    }

    /**
     * Rejeter une demande d'inscription
     */
    @DeleteMapping("/pending/{id}")
    public ResponseEntity<?> rejectPendingUser(@PathVariable Long id) {
        try {
            adminUserService.rejectPendingUser(id);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Demande d'inscription rejetée avec succès"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Créer un nouvel administrateur
     */
    @PostMapping("/admin")
    public ResponseEntity<?> createAdmin(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            User newAdmin = adminUserService.createAdmin(email, password);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Administrateur créé avec succès",
                    "user", Map.of(
                            "id", newAdmin.getId(),
                            "email", newAdmin.getEmail(),
                            "roles", newAdmin.getRoles()
                    )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Créer un nouvel évaluateur
     */
    @PostMapping("/evaluateur")
    public ResponseEntity<?> createEvaluateur(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            User newEvaluateur = adminUserService.createEvaluateur(email, password);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Évaluateur créé avec succès",
                    "user", Map.of(
                            "id", newEvaluateur.getId(),
                            "email", newEvaluateur.getEmail(),
                            "roles", newEvaluateur.getRoles()
                    )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Changer le rôle d'un utilisateur
     */
    @PutMapping("/{id}/role")
    public ResponseEntity<?> changeUserRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newRole = request.get("role");
            User updatedUser = adminUserService.changeUserRole(id, newRole);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Rôle mis à jour avec succès",
                    "user", Map.of(
                            "id", updatedUser.getId(),
                            "email", updatedUser.getEmail(),
                            "roles", updatedUser.getRoles()
                    )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Activer ou désactiver un utilisateur
     */
    @PutMapping("/{id}/activation")
    public ResponseEntity<?> setUserActivation(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        try {
            Boolean activated = request.get("activated");
            User updatedUser = adminUserService.setUserActivation(id, activated);

            String action = activated ? "activé" : "désactivé";
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Utilisateur " + action + " avec succès",
                    "user", Map.of(
                            "id", updatedUser.getId(),
                            "email", updatedUser.getEmail(),
                            "active", updatedUser.isValidated()
                    )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Supprimer un utilisateur
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            adminUserService.deleteUser(id);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Utilisateur supprimé avec succès"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Modifier l'email d'un utilisateur
     */
    @PutMapping("/{id}/email")
    public ResponseEntity<?> changeUserEmail(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newEmail = request.get("email");
            User updatedUser = adminUserService.changeUserEmail(id, newEmail);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Email mis à jour avec succès",
                    "user", Map.of(
                            "id", updatedUser.getId(),
                            "email", updatedUser.getEmail()
                    )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "error", e.getMessage()
            ));
        }
    }
}