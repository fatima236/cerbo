package com.example.cerbo.controller;
import com.example.cerbo.dto.SignupRequest;
import com.example.cerbo.entity.User;
import com.example.cerbo.service.UserService;
import com.example.cerbo.dto.LoginRequest;
import com.example.cerbo.dto.JwtResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @PostMapping("/login")
    public ResponseEntity<?> loginAdmin(@RequestBody LoginRequest loginRequest) {
        User user = userService.findByEmail(loginRequest.getEmail());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Utilisateur non trouvé");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Mot de passe incorrect");
        }

        // Vérification flexible des rôles
        boolean isAdmin = false;
        if (user.getRoles() != null) {
            for (String role : user.getRoles()) {
                // Accepte ADMIN, Role_ADMIN, etc.
                if (role != null && role.toUpperCase().contains("ADMIN")) {
                    isAdmin = true;
                    break;
                }
            }
        }

        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux admins");
        }

        String token = "fake-jwt-token-" + user.getEmail();
        return ResponseEntity.ok(new JwtResponse(token, "ADMIN"));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest) {
        if (userService.findByEmail(signupRequest.getEmail()) != null) {
            return ResponseEntity.status(400).body("Cet utilisateur existe déjà !");
        }

        User newUser = userService.createUser(
                signupRequest.getEmail(),
                signupRequest.getPassword(),
                Set.of(signupRequest.getRole())
        );

        return ResponseEntity.ok("Compte créé avec succès !");
    }

}