package com.example.cerbo.controller;

import com.example.cerbo.dto.*;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.UserRepository;
import com.example.cerbo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

            // Générer l'access token
            String accessToken = jwtTokenUtil.generateToken(authentication);

            // Générer un refresh token
            String refreshToken = jwtTokenUtil.generateRefreshToken(authentication);

            return ResponseEntity.ok(Map.of(
                    "token", accessToken,
                    "refreshToken", refreshToken, // Retourner aussi le refresh token
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
    @PreAuthorize("hasRole('EVALUATEUR')")
    @PostMapping("/loginevaluateur")
    public ResponseEntity<?> loginEvaluateur(@RequestBody LoginRequest loginRequest) {
        try {
            // Authentifier l'utilisateur
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Récupérer l'utilisateur depuis la base
            User user = userRepository.findByEmail(loginRequest.getEmail());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Utilisateur non trouvé"));
            }

            // Vérifier que l'utilisateur a le rôle EVALUATEUR
            if (!user.getRoles().contains("EVALUATEUR")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès réservé aux évaluateurs"));
            }

            // Générer un token JWT
            String token = jwtTokenUtil.generateToken(authentication);

            // Retourner une réponse JSON compatible avec le frontend
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", "EVALUATEUR",
                    "email", user.getEmail(),
                    "expiresIn", jwtTokenUtil.getExpiration()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Email ou mot de passe incorrect"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur serveur: " + e.getMessage()));
        }
    }





    // [Autres méthodes existantes...]


    // Injection par constructeur

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        try {
            // 1. Validation du refresh token
            if (!jwtTokenUtil.validateRefreshToken(refreshToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalide");
            }

            // 2. Récupération de l'utilisateur
            String username = jwtTokenUtil.getUsernameFromRefreshToken(refreshToken);
            User user = userRepository.findByEmail(username);

            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé");
            }

            // 3. Création de l'authentication
            List<GrantedAuthority> authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    user.getEmail(),
                    null,
                    authorities);

            // 4. Génération des nouveaux tokens
            String newAccessToken = jwtTokenUtil.generateToken(auth);
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(auth);

            // 5. Retour de la réponse
            return ResponseEntity.ok(Map.of(
                    "token", newAccessToken,
                    "refreshToken", newRefreshToken,
                    "expiresIn", jwtTokenUtil.getAccessTokenExpiration()
            ));

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Échec du rafraîchissement: " + e.getMessage()
            );
        }
    }

}