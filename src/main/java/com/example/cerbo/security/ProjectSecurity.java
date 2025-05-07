package com.example.cerbo.security;

import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectSecurity {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public boolean isProjectMember(Long projectId, Authentication authentication) {
        log.debug("Vérification accès pour projet {} - utilisateur {}",
                projectId, authentication.getName());
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        User user = userRepository.findByEmail(authentication.getName());
        if (user == null) {
            return false;
        }

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return false;
        }

        // Admin peut tout voir
        if (user.getRoles().contains("ADMIN")) {
            return true;
        }

        // Investigateur principal ou co-investigateur
        if (project.getPrincipalInvestigator().equals(user) ||
                project.getInvestigators().contains(user)) {
            return true;
        }

        // Evaluateur assigné
        if (project.getReviewers().contains(user)) {
            return true;
        }

        return false;
    }

    public boolean isProjectReviewer(Long projectId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        User user = userRepository.findByEmail(authentication.getName());
        if (user == null) {
            return false;
        }

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return false;
        }

        return project.getReviewers().contains(user);
    }
}