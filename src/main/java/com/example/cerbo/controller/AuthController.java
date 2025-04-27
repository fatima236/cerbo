package com.example.cerbo.controller;

import com.example.cerbo.dto.JwtTokenUtil;
import com.example.cerbo.dto.LoginRequest;
import com.example.cerbo.dto.SignupRequest;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.UserRepository;
import com.example.cerbo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PostMapping("/loginadmin")
    public ResponseEntity<?> loginAdmin(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Version sans Optional
            User user = userRepository.findByEmail(loginRequest.getEmail());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Utilisateur non trouvé"));
            }

            if (!user.getRoles().contains("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès réservé aux administrateurs"));
            }

            String token = jwtTokenUtil.generateToken(authentication);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", "ADMIN",
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

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest) {
        User existingUser = userRepository.findByEmail(signupRequest.getEmail());
        if (existingUser != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Cet utilisateur existe déjà !"));
        }

        User newUser = userService.createUser(
                signupRequest.getEmail(),
                signupRequest.getPassword(),
                Set.of(signupRequest.getRole())
        );

        return ResponseEntity.ok(Map.of(
                "message", "Compte créé avec succès !",
                "email", newUser.getEmail()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User userp = userService.findByEmail(user.getEmail());
        if (userp == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> response = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "firstName", user.getPrenom(),
                "lastName", user.getNom(),
                "roles", user.getRoles()
        );

        return ResponseEntity.ok(response);
    }
}