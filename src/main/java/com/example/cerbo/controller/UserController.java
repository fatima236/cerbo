package com.example.cerbo.controller;

import com.example.cerbo.dto.JwtResponse;
import com.example.cerbo.dto.LoginRequest;
import com.example.cerbo.dto.SignupRequest;
import com.example.cerbo.entity.User;
import com.example.cerbo.service.UserService;
import com.example.cerbo.dto.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PostMapping("/request-investigateur")
    public ResponseEntity<?> requestInvestigateurSignup(@RequestBody SignupRequest request) {
        try {
            User userRequest = new User();
            userRequest.setEmail(request.getEmail());
            userRequest.setPassword(request.getPassword());

            userService.requestInvestigateurSignup(userRequest);

            return ResponseEntity.ok(Map.of(
                    "status", "PENDING",
                    "message", "Votre demande a été envoyée. Un administrateur doit la valider."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Erreur lors du traitement de votre demande"
            ));
        }
    }

    @GetMapping("/approve/{pendingUserId}")
    public ResponseEntity<String> approveInvestigateur(@PathVariable Long pendingUserId) {
        try {
            User approvedUser = userService.approveInvestigateur(pendingUserId);

            String responseHtml = "<html>" +
                    "<body style=\"font-family: Arial, sans-serif;\">" +
                    "<h1 style=\"color: #4CAF50;\">Demande approuvée avec succès</h1>" +
                    "<p>L'utilisateur <strong>" + approvedUser.getEmail() + "</strong> a été créé.</p>" +
                    "<p>Un email de confirmation a été envoyé à l'investigateur.</p>" +
                    "<a href=\"http://localhost:3000/admin\" " +
                    "style=\"color: #2e6c80; text-decoration: none;\">" +
                    "Retour au tableau de bord" +
                    "</a>" +
                    "</body>" +
                    "</html>";

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(responseHtml);
        } catch (Exception e) {
            String errorHtml = "<html>" +
                    "<body style=\"font-family: Arial, sans-serif;\">" +
                    "<h1 style=\"color: #f44336;\">Erreur lors de l'approbation</h1>" +
                    "<p>" + e.getMessage() + "</p>" +
                    "</body>" +
                    "</html>";

            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorHtml);
        }
    }

    @GetMapping("/reject/{pendingUserId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String rejectInvestigateurHtml(@PathVariable Long pendingUserId) {
        try {
            userService.rejectInvestigateur(pendingUserId);
            return "<html><body><h1>Demande rejetée</h1><p>La demande a été supprimée.</p></body></html>";
        } catch (Exception e) {
            return "<html><body><h1>Erreur</h1><p>" + e.getMessage() + "</p></body></html>";
        }
    }

    @PostMapping("/logininv")
    public ResponseEntity<?> loginInvestigateur(@RequestBody LoginRequest loginRequest) {
        User user = userService.findByEmail(loginRequest.getEmail());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Utilisateur non trouvé");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Mot de passe incorrect");
        }

        // Vérification spécifique du rôle
        if (!user.getRoles().contains("INVESTIGATEUR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux investigateurs");
        }

        String fakeJwt = "fake-jwt-token-" + user.getEmail();
        return ResponseEntity.ok(new JwtResponse(fakeJwt, "INVESTIGATEUR"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        try {
            userService.requestPasswordReset(email);
            return ResponseEntity.ok(Map.of(
                    "message", "Un email de réinitialisation a été envoyé",
                    "status", "EMAIL_SENT"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");

            userService.resetPassword(token, newPassword);
            return ResponseEntity.ok(Map.of(
                    "message", "Mot de passe mis à jour avec succès",
                    "status", "PASSWORD_RESET"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}