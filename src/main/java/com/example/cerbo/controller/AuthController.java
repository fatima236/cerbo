package com.example.cerbo.controller;
import com.example.cerbo.dto.SignupRequest;
import com.example.cerbo.entity.User;
import com.example.cerbo.service.UserService;
import com.example.cerbo.dto.LoginRequest;
import com.example.cerbo.dto.JwtResponse;
import org.apache.catalina.connector.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    @PostMapping("/loginadmin")
    public ResponseEntity<?> loginEvl(@RequestBody LoginRequest loginRequest) {
        User user = userService.findByEmail(loginRequest.getEmail());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Utilisateur non trouvé");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Mot de passe incorrect");
        }

        if (!user.getRoles().contains("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux ADMIN");
        }

        String fakeJwt = "fake-jwt-token-" + user.getEmail();

        // Créez simplement un objet qui contient à la fois le JwtResponse ET l'email
        Map<String, Object> response = new HashMap<>();
        response.put("token", fakeJwt);
        response.put("role", "ADMIN");
        response.put("email", user.getEmail()); // Juste ajouté cette ligne

        // Retournez explicitement l'email dans la réponse
        return ResponseEntity.ok(Map.of(
                "token", fakeJwt,
                "role", "ADMIN",
                "email", user.getEmail() // Cette ligne est cruciale
        ));
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