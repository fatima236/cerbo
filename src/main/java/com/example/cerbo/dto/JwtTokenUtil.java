package com.example.cerbo.dto;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtTokenUtil {


    private final SecretKey refreshTokenSecretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    // Assurez-vous que la clé correspond EXACTEMENT à celle du client
    private final SecretKey accessTokenSecretKey = Keys.hmacShaKeyFor(
            "votre-cle-secrete-pour-les-access-tokens-d-au-moins-64-caracteres-1234567890abcdef".getBytes()
    );
    public JwtTokenUtil(
            @Value("${jwt.access.secret}") String accessSecret,
            @Value("${jwt.refresh.secret}") String refreshSecret,
            @Value("${jwt.access.expiration}") long accessExpiration,
            @Value("${jwt.refresh.expiration}") long refreshExpiration) {

        if (accessSecret == null || accessSecret.length() < 64) {
            throw new IllegalArgumentException("La clé secrète pour les access tokens doit contenir au moins 64 caractères");
        }
        if (refreshSecret == null || refreshSecret.length() < 64) {
            throw new IllegalArgumentException("La clé secrète pour les refresh tokens doit contenir au moins 64 caractères");
        }


        this.refreshTokenSecretKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessExpiration;
        this.refreshTokenExpiration = refreshExpiration;
    }

    // Génère un token à partir d'une Authentication
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return buildToken(username, roles, accessTokenSecretKey, accessTokenExpiration);
    }

    // Génère un token à partir d'un username et roles
    public String generateToken(String username, Collection<String> roles) {
        return buildToken(username, roles, accessTokenSecretKey, accessTokenExpiration);
    }

    // Génère un refresh token à partir d'une Authentication
    public String generateRefreshToken(Authentication authentication) {
        return buildToken(
                authentication.getName(),
                Collections.emptyList(), // Pas de rôles dans le refresh token
                refreshTokenSecretKey,
                refreshTokenExpiration
        );
    }

    // Génère un refresh token à partir d'un username et roles
    public String generateRefreshToken(String username, Collection<String> roles) {
        return buildToken(username, roles, refreshTokenSecretKey, refreshTokenExpiration);
    }

    // Méthode privée pour construire les tokens
    private String buildToken(String username, Collection<String> roles, SecretKey key, long expiration) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256) // Changé de HS512 à HS256
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token, accessTokenSecretKey).getSubject();
    }

    public String getUsernameFromRefreshToken(String token) {
        return getClaimsFromToken(token, refreshTokenSecretKey).getSubject();
    }

    public boolean validateToken(String token) {
        return validateToken(token, accessTokenSecretKey);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshTokenSecretKey);
    }

    private boolean validateToken(String token, SecretKey key) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    public SecretKey getAccessTokenSecretKey() {
        return this.accessTokenSecretKey;
    }
    public Claims getClaimsFromToken(String token, SecretKey key) {
        return Jwts.parserBuilder()
                .setSigningKey(key) // Corrigé "setSigningKey" (il manquait un 'n')
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    // Alias pour compatibilité
    public long getExpiration() {
        return getAccessTokenExpiration();
    }

    public String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    public String getEmailFromToken(String token) {
        return getUsernameFromToken(token); // Alias pour compatibilité
    }
}