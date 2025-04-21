package com.example.cerbo.controller;

import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.ProjectStatus;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.RemarkRepository;
import com.example.cerbo.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:3000")
@AllArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final RemarkRepository remarkRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(projectRepository.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATEUR', 'INVESTIGATEUR')")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('INVESTIGATEUR')")
    public ResponseEntity<Project> createProject(@Valid @RequestBody Project project) {
        Project savedProject = projectRepository.save(project);
        return new ResponseEntity<>(savedProject, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/assign-evaluator")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignEvaluator(
            @PathVariable Long id,
            @RequestBody Map<String, Long> request
    ) {
        Long evaluatorId = request.get("evaluatorId");

        Optional<Project> projectOpt = projectRepository.findById(id);
        Optional<User> evaluatorOpt = userRepository.findById(evaluatorId);

        if (projectOpt.isEmpty() || evaluatorOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Projet ou évaluateur non trouvé"
            ));
        }

        Project project = projectOpt.get();
        User evaluator = evaluatorOpt.get();

        // Vérifier si l'utilisateur est un évaluateur
        if (!evaluator.getRoles().contains("EVALUATEUR")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "L'utilisateur sélectionné n'est pas un évaluateur"
            ));
        }

        // Ajouter l'évaluateur au projet
        Set<User> reviewers = project.getReviewers();
        reviewers.add(evaluator);
        project.setReviewers(reviewers);

        projectRepository.save(project);

        return ResponseEntity.ok(Map.of(
                "message", "Évaluateur assigné avec succès"
        ));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {
        String statusStr = request.get("status");
        String comment = request.get("comment");

        if (statusStr == null || comment == null || comment.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Le statut et le commentaire sont requis"
            ));
        }

        Optional<Project> projectOpt = projectRepository.findById(id);

        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Project project = projectOpt.get();

        try {
            ProjectStatus status = ProjectStatus.valueOf(statusStr);
            project.setStatus(status);

            // Créer une remarque pour le changement de statut
            Remark remark = new Remark();
            remark.setContent("Changement de statut vers " + status.name() + ": " + comment);
            remark.setCreationDate(LocalDateTime.now());
            remark.setProject(project);

            // Récupérer l'administrateur qui fait la mise à jour
            // Note: Vous devriez idéalement récupérer l'utilisateur connecté à partir du contexte de sécurité
            User admin = userRepository.findByEmail("admin@example.com");
            remark.setReviewer(admin);

            remarkRepository.save(remark);
            projectRepository.save(project);

            return ResponseEntity.ok(Map.of(
                    "message", "Statut mis à jour avec succès"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Statut invalide"
            ));
        }
    }
}