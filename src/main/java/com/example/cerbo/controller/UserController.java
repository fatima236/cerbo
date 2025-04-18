package com.example.cerbo.controller;

import com.example.cerbo.dto.JwtResponse;
import com.example.cerbo.dto.LoginRequest;
import com.example.cerbo.dto.SignupRequest;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.UserRepository;
import com.example.cerbo.service.UserService;
import com.example.cerbo.dto.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
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
                    "<a href=\"http://localhost:3000/investigateur/dashboard\" " +
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
    @Autowired
    private UserRepository userRepository;
    @PostMapping("/logininv")
    public ResponseEntity<?> loginInvestigateur(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            User user = userRepository.findByEmail(loginRequest.getEmail());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("{\"error\":\"Utilisateur non trouvé\"}");
            }

            if (!user.getRoles().contains("INVESTIGATEUR")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("{\"error\":\"Accès réservé aux investigateurs\"}");
            }

            String token = jwtTokenUtil.generateToken(authentication);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", "INVESTIGATEUR",
                    "email", user.getEmail(),
                    "expiresIn", jwtTokenUtil.getExpiration()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\":\"Email ou mot de passe incorrect\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"Erreur serveur: " + e.getMessage() + "\"}");
        }
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
    // Ajoutez cette méthode dans UserController.java
    @PostMapping("/logoutinv")
    @PreAuthorize("hasRole('INVESTIGATEUR')") // Add this annotation
    public ResponseEntity<?> logoutInvestigateur(HttpServletRequest request) {
        // Get the token from the request
        String token = jwtTokenUtil.getTokenFromRequest(request);

        // In a real implementation, you might want to blacklist the token
        // For this demo, we'll just return a success response

        return ResponseEntity.ok(Map.of(
                "message", "Déconnexion réussie",
                "status", "LOGOUT_SUCCESS"
        ));
    }

    @PreAuthorize("hasRole('INVESTIGATEUR')")
    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)

    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestPart(value = "firstName", required = false) String firstName,
            @RequestPart(value = "lastName", required = false) String lastName,
            @RequestPart(value = "phone", required = false) String phone,
            @RequestPart(value = "bio", required = false) String bio,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            User updatedUser = userService.updateProfile(email, firstName, lastName, phone, bio, photo);

            Map<String, Object> response = new HashMap<>();
            response.put("firstName", updatedUser.getFirstName());
            response.put("lastName", updatedUser.getLastName());
            response.put("phone", updatedUser.getPhone());
            response.put("bio", updatedUser.getBio());
            response.put("photo", updatedUser.getPhoto() != null ? updatedUser.getPhoto() : "/user-avatar.jpg");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
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