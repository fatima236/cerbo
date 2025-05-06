package com.example.cerbo.config;

import com.example.cerbo.dto.JwtTokenFilter;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Lazy
    private final JwtTokenFilter jwtTokenFilter;

    public SecurityConfig(JwtTokenFilter jwtTokenFilter) {
        this.jwtTokenFilter = jwtTokenFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
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

                        // Auth endpoints accessibles publiquement
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()

                        // Public GET
                        .requestMatchers(HttpMethod.GET, "/api/articles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trainings").permitAll()

                        // JWT protected
                        .requestMatchers("/api/profile").authenticated()
                        .requestMatchers("/api/notifications").authenticated()
                        .requestMatchers("/api/meetings/**").authenticated()
                        .requestMatchers("/api/admin/users", "/api/admin/users/**", "/api/admin/users/pending", "/api/admin/users/pending/**").authenticated()

                        // Projects
                        .requestMatchers("/api/projects", "/api/projects/**").hasAnyRole("ADMIN", "INVESTIGATEUR", "EVALUATEUR")
                        .requestMatchers("/api/projects/documents/**", "/api/projects/documents/download", "/api/projects/documents/content").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/projects/evaluators").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/projects/assign-evaluators").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/projects/evaluators/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/projects/investigator").hasRole("INVESTIGATEUR")
                        .requestMatchers(HttpMethod.GET, "/api/projects/investigator/**").hasRole("INVESTIGATEUR")

                        .requestMatchers(HttpMethod.POST, "/api/projects/remarks").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/projects/remarks/response").authenticated()


                        // Articles
                        .requestMatchers(HttpMethod.POST, "/api/articles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/articles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/articles/**").hasRole("ADMIN")

                        // Events
                        .requestMatchers(HttpMethod.POST, "/api/events/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/events/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasRole("ADMIN")

                        // Trainings
                        .requestMatchers(HttpMethod.POST, "/api/trainings/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/trainings/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/trainings/**").hasRole("ADMIN")

                        // Role-specific dashboards
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/evaluateur/**").hasRole("EVALUATEUR")
                        .requestMatchers("/investigateur/**").hasRole("INVESTIGATEUR")

                        // Tout le reste
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