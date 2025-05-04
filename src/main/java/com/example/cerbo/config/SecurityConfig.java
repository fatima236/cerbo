package com.example.cerbo.config;

import com.example.cerbo.dto.JwtTokenFilter;
import com.example.cerbo.dto.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    @Value("${jwt.access.secret}")
    private String accessTokenSecret;

    @Value("${jwt.refresh.secret}")
    private String refreshTokenSecret;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpiration;

    @Lazy
    private final JwtTokenFilter jwtTokenFilter;

    public SecurityConfig(JwtTokenFilter jwtTokenFilter) {
        this.jwtTokenFilter = jwtTokenFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Configuration CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000")); // Autoriser le frontend
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // Autoriser l'accès aux routes d'authentification
                        .requestMatchers("/api/profile").authenticated()
                        .requestMatchers("/api/notifications").authenticated()
                        .requestMatchers("/api/meetings/**").authenticated()

                        .requestMatchers("/api/admin/users").authenticated()
                        .requestMatchers("/api/admin/users/**").authenticated()
                        .requestMatchers("/api/admin/users/pending").authenticated()
                        .requestMatchers("/api/admin/users/pending/**").authenticated()

                        .requestMatchers("/api/events").permitAll()
                        .requestMatchers("/api/articles").permitAll()
                        .requestMatchers("/api/trainings").permitAll()
                        // Routes protégées

                        .requestMatchers("/api/projects").hasAnyRole("ADMIN", "INVESTIGATEUR", "EVALUATEUR")
                        .requestMatchers("/api/projects/**").hasAnyRole("ADMIN", "INVESTIGATEUR", "EVALUATEUR")
                        .requestMatchers("/api/projects/**/documents/**").authenticated()
                        .requestMatchers("/api/projects/**/documents/**/download").authenticated()
                        .requestMatchers("/api/projects/**/documents/**/content").authenticated()


                        .requestMatchers(HttpMethod.GET, "/api/projects/evaluators").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/projects/assign-evaluators").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/projects/evaluators/**").hasRole("ADMIN")


                        .requestMatchers("/api/events/**").hasRole("ADMIN")
                        .requestMatchers("/api/articles/**").hasRole("ADMIN")
                        .requestMatchers("/api/trainings/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/evaluateur/**").hasAuthority("ROLE_EVALUATEUR")
                        .requestMatchers("/investigateur/**").hasRole("INVESTIGATEUR")
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
