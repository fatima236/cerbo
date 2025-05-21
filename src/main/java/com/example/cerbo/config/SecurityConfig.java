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
                        .requestMatchers(HttpMethod.GET, "/api/meetings/evaluators").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/meetings/{meetingId}/attendance").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/meetings/{meetingId}/attendance").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/meetings/evaluators/stats").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/meetings/evaluators/{evaluatorId}").hasRole("ADMIN")
                        .requestMatchers("/api/admin/users", "/api/admin/users/**", "/api/admin/users/pending", "/api/admin/users/pending/**").authenticated()

                        // Projects
                        .requestMatchers("/api/projects", "/api/projects/**").hasAnyRole("ADMIN", "INVESTIGATEUR", "EVALUATEUR")
                        .requestMatchers("/api/projects/documents/**", "/api/projects/documents/download", "/api/projects/documents/content").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/projects/evaluators").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/projects/assign-evaluators").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/projects/evaluators/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/projects/investigator").hasAnyRole("ADMIN", "INVESTIGATEUR", "EVALUATEUR")
                        .requestMatchers(HttpMethod.GET, "/api/projects/investigator/**").hasAnyRole("ADMIN", "INVESTIGATEUR", "EVALUATEUR")

                        .requestMatchers(HttpMethod.POST, "/api/projects/remarks").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/projects/remarks/response").authenticated()

                        // Endpoints admin pour les remarques
                        .requestMatchers(HttpMethod.GET, "/api/admin/remarks/pending").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/stats").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/remarks/projects/{projectId}/final-evaluations").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/remarks/projects/{projectId}/evaluations-by-document").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/remarks/{documentReviewId}/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/remarks/{documentReviewId}/content").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/remarks/projects/{projectId}/organized").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/remarks/projects/{projectId}/validated").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/remarks/projects/{projectId}/generate-report").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/admin/projects/{projectId}/report/preview").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/projects/{projectId}/report/preview").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/projects/{projectId}/report/send").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/projects/{projectId}/report/download").hasRole("INVESTIGATEUR")
                        .requestMatchers(HttpMethod.GET, "api/admin/projects/${project.id}/report/validated-remarks-grouped").hasRole("INVESTIGATEUR")


                        .requestMatchers(HttpMethod.GET, "/api/projects/assigned-to-me").hasAnyRole("ADMIN", "EVALUATEUR")
                        .requestMatchers(HttpMethod.GET, "/api/projects/{projectId}/documents").hasAnyRole("ADMIN", "EVALUATEUR")
                        .requestMatchers(HttpMethod.GET, "/api/projects/{projectId}/documents/{documentName}/content").hasAnyRole("ADMIN", "EVALUATEUR")
                        .requestMatchers(HttpMethod.GET, "/api/projects/{projectId}/documents/{documentId}/reviews/me").hasRole("EVALUATEUR")
                        .requestMatchers(HttpMethod.GET, "/api/projects/{projectId}/documents/reviews/me").hasRole("EVALUATEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/projects/{projectId}/documents/{documentId}/review").hasAnyRole("ADMIN", "EVALUATEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/projects/{projectId}/documents/{documentId}/clear-review").hasRole("EVALUATEUR")
                        .requestMatchers(HttpMethod.POST, "/api/projects/{projectId}/documents/submit-review").hasAnyRole("ADMIN", "EVALUATEUR")
                        .requestMatchers(HttpMethod.GET, "/api/projects/{projectId}/documents/my-reviews").hasAnyRole("ADMIN", "EVALUATEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/projects/{projectId}/documents/set-deadline").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/projects/{projectId}/submission-status").hasRole("EVALUATEUR")
                        .requestMatchers(HttpMethod.GET, "/api/investigator/reports/project/{projectId}").hasRole("INVESTIGATEUR")



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

                        // Endpoints spécifiques ADMINSYS
                        .requestMatchers("/api/adminsys/**").hasRole("ADMINSYS")
                        .requestMatchers("/api/adminsys/audit-logs").hasRole("ADMINSYS")
                        .requestMatchers("/api/adminsys/users").hasRole("ADMINSYS")

                        // meetings
                        .requestMatchers("/api/meetings/{meetingId}/agenda/**").hasRole("ADMIN")
                        .requestMatchers("/api/meetings/{meetingId}/attendees/**").hasRole("ADMIN")
                        .requestMatchers("/api/meetings/{meetingId}/attendance/**").hasAnyRole("ADMIN", "EVALUATEUR")

                        // Endpoints existants modifiés pour ADMINSYS
                        .requestMatchers("/api/admin/users", "/api/admin/users/**").hasAnyRole("ADMIN", "ADMINSYS")
                        .requestMatchers("/api/admin/audit-logs").hasAnyRole("ADMIN", "ADMINSYS")
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