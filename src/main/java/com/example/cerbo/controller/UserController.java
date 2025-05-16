package com.example.cerbo.controller;

import com.example.cerbo.dto.*;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.UserRepository;
import com.example.cerbo.service.FileStorageService;
import com.example.cerbo.service.UserDetailsServiceImp;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserDetailsServiceImp userDetailsServiceImp;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired

    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PostMapping(value = "/request-investigateur", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> requestInvestigateurSignup(
            @RequestPart("userData") String userDataStr,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            SignupRequest request = objectMapper.readValue(userDataStr, SignupRequest.class);

            User userRequest = new User();
            userRequest.setEmail(request.getEmail());
            userRequest.setPassword(request.getPassword());
            userRequest.setCivilite(request.getCivilite());
            userRequest.setNom(request.getNom());
            userRequest.setPrenom(request.getPrenom());
            userRequest.setTitre(request.getTitre());
            userRequest.setLaboratoire(request.getLaboratoire());
            userRequest.setAffiliation(request.getAffiliation());

            if (photo != null && !photo.isEmpty()) {
                String fileName = fileStorageService.storeFile(photo);
                userRequest.setPhotoUrl(fileName);
            }

            userDetailsServiceImp.requestInvestigateurSignup(userRequest);

            return ResponseEntity.ok(Map.of(
                    "status", "PENDING",
                    "message", "Votre demande a été envoyée. Un administrateur doit la valider."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Erreur lors du traitement de votre demande: " + e.getMessage()
            ));
        }
    }
    @GetMapping("/approve/{pendingUserId}")
    public ResponseEntity<String> approveInvestigateur(@PathVariable Long pendingUserId) {
        try {
            User approvedUser = userDetailsServiceImp.approveInvestigateur(pendingUserId);

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
            userDetailsServiceImp.rejectInvestigateur(pendingUserId);
            return "<html><body><h1>Demande rejetée</h1><p>La demande a été supprimée.</p></body></html>";
        } catch (Exception e) {
            return "<html><body><h1>Erreur</h1><p>" + e.getMessage() + "</p></body></html>";
        }
    }
    @Autowired
    private UserRepository userRepository;


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        try {
            userDetailsServiceImp.requestPasswordReset(email);
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

            userDetailsServiceImp.resetPassword(token, newPassword);
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


    @PostMapping("/send-verification")
    public ResponseEntity<?> sendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        try {
            userDetailsServiceImp.sendVerificationCode(email);
            return ResponseEntity.ok(Map.of(
                    "message", "Code de vérification envoyé",
                    "status", "CODE_SENT"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        try {
            boolean isValid = userDetailsServiceImp.verifyCode(email, code);
            if (!isValid) {
                throw new IllegalArgumentException("Code invalide ou expiré");
            }
            return ResponseEntity.ok(Map.of(
                    "message", "Email vérifié avec succès",
                    "status", "EMAIL_VERIFIED"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean exists = userDetailsServiceImp.checkEmailExists(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }



    // [Autres méthodes existantes...]


    // Injection par constructeur


}