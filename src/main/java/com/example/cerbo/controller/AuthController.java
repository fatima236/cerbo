package com.example.cerbo.controller;

import com.example.cerbo.dto.JwtTokenUtil;
import com.example.cerbo.dto.LoginRequest;
import com.example.cerbo.dto.SignupRequest;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.UserRepository;
import com.example.cerbo.service.UserDetailsServiceImp;
import com.example.cerbo.service.blacklistService.BlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private UserDetailsServiceImp userDetailsServiceImp;

    @Autowired
    BlacklistService blacklistService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            User user = userRepository.findByEmail(loginRequest.getEmail());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Utilisateur non trouvé"));
            }

            if (!user.isValidated()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Votre compte n'est pas encore validé."));
            }

            String token = jwtTokenUtil.generateToken(authentication);
            String refreshToken = jwtTokenUtil.generateRefreshToken(authentication);

            // récupérer le premier rôle (ou envoyer la liste si tu veux)
            String role = user.getRoles().stream().findFirst().orElse("UNKNOWN");

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", role, // retourne ici INVESTIGATEUR, ADMIN, EVA, etc.
                    "refreshToken", refreshToken,
                    "email", user.getEmail(),
                    "userId",user.getId(),
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

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> payload) {
        String refreshToken = payload.get("refreshToken");

        if (refreshToken == null || !jwtTokenUtil.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token invalide ou expiré");
        }

        String username = jwtTokenUtil.getUsernameFromRefreshToken(refreshToken);
        User optionalUser = userRepository.findByEmail(username);

        if (optionalUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utilisateur non trouvé");
        }

        String newAccessToken = jwtTokenUtil.generateAccessToken(optionalUser);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", newAccessToken);

        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");

        blacklistService.add(token);

        return ResponseEntity.ok("Déconnecté avec succès.");
    }


    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest) {
        User existingUser = userRepository.findByEmail(signupRequest.getEmail());
        if (existingUser != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Cet utilisateur existe déjà !"));
        }

        User newUser = userDetailsServiceImp.createUser(
                signupRequest.getEmail(),
                signupRequest.getPassword(),
                Set.of(signupRequest.getRole())
        );

        return ResponseEntity.ok(Map.of(
                "message", "Compte créé avec succès !",
                "email", newUser.getEmail()
        ));
    }


}