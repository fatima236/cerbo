package com.example.cerbo.dto;

import com.example.cerbo.service.UserDetailsServiceImp;
import com.example.cerbo.service.blacklistService.BlacklistService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
    public static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);
    private final UserDetailsServiceImp userDetailsServiceImp;
    private final JwtTokenUtil jwtTokenUtil;
    private final BlacklistService blacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = jwtTokenUtil.getTokenFromRequest(request);

        if (token != null) {

            // Vérifie si le token est blacklisté
            if (blacklistService.isBlacklisted(token)) {
                logger.warn("Token is blacklisted!");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalidé (déconnecté)");
                return;
            }

            // Vérifie si le token est invalide ou expiré
            if (!jwtTokenUtil.validateToken(token)) {
                logger.warn("Token expiré ou invalide !");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expiré ou invalide");
                return;
            }

            // Token est valide : on extrait les infos
            String email = jwtTokenUtil.getUsernameFromToken(token);
            Claims claims = jwtTokenUtil.getClaimsFromToken(token, jwtTokenUtil.getAccessTokenSecretKey());
            List<String> roles = claims.get("roles", List.class);

            // Si l'utilisateur n'est pas encore authentifié, on l'authentifie
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsServiceImp.loadUserByUsername(email);
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        authorities
                );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        // Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }
}
