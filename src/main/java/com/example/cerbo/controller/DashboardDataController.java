package com.example.cerbo.controller;

import com.example.cerbo.repository.*;
import com.example.cerbo.entity.enums.ProjectStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardDataController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PendingUserRepository pendingUserRepository;

    @GetMapping("/performance")
    public Map<String, Object> getPerformanceData() {
        // Récupérer les données des 6 derniers mois
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = month.atEndOfMonth().atTime(23, 59, 59);

            // Compter les projets soumis ce mois-ci
            long projectsCount = projectRepository.findAll().stream()
                    .filter(p -> p.getSubmissionDate() != null)
                    .filter(p -> p.getSubmissionDate().isAfter(startOfMonth) && p.getSubmissionDate().isBefore(endOfMonth))
                    .count();

            // Compter les nouveaux utilisateurs ce mois-ci
            long usersCount = userRepository.findAll().stream()
                    .filter(u -> u.getId() != null) // Simple filter, vous pouvez ajouter une date de création
                    .count() / 6; // Simulation - vous devriez avoir une date de création

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("name", getMonthName(month.getMonthValue()));
            monthData.put("projets", projectsCount);
            monthData.put("utilisateurs", usersCount);

            monthlyData.add(monthData);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("monthlyData", monthlyData);

        return response;
    }

    @GetMapping("/alerts")
    public Map<String, Object> getAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();

        // 1. Projets en attente depuis plus de 7 jours
        long oldPendingProjects = projectRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProjectStatus.SOUMIS)
                .filter(p -> p.getSubmissionDate().isBefore(LocalDateTime.now().minusDays(7)))
                .count();

        if (oldPendingProjects > 0) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("id", 1);
            alert.put("type", "warning");
            alert.put("title", "Projets en attente");
            alert.put("message", oldPendingProjects + " projets nécessitent une révision depuis plus de 7 jours");
            alert.put("action", "Voir les projets");
            alert.put("priority", "high");
            alerts.add(alert);
        }

        // 2. Demandes d'inscription en attente
        long pendingUsers = pendingUserRepository.count();
        if (pendingUsers > 0) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("id", 2);
            alert.put("type", "info");
            alert.put("title", "Demandes d'inscription");
            alert.put("message", pendingUsers + " demandes d'inscription en attente");
            alert.put("action", "Traiter les demandes");
            alert.put("priority", "medium");
            alerts.add(alert);
        }

        // 3. Projets avec deadlines proches (dans les 3 prochains jours)
        long urgentDeadlines = projectRepository.findAll().stream()
                .filter(p -> p.getResponseDeadline() != null)
                .filter(p -> p.getResponseDeadline().isBefore(LocalDateTime.now().plusDays(3)))
                .filter(p -> p.getResponseDeadline().isAfter(LocalDateTime.now()))
                .count();

        if (urgentDeadlines > 0) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("id", 3);
            alert.put("type", "warning");
            alert.put("title", "Deadlines proches");
            alert.put("message", urgentDeadlines + " projets ont une deadline dans moins de 3 jours");
            alert.put("action", "Voir les deadlines");
            alert.put("priority", "high");
            alerts.add(alert);
        }

        // 4. Message de succès si tout va bien
        if (alerts.isEmpty()) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("id", 4);
            alert.put("type", "success");
            alert.put("title", "Tout va bien !");
            alert.put("message", "Aucune alerte importante en ce moment");
            alert.put("action", "Continuer");
            alert.put("priority", "low");
            alerts.add(alert);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alerts);
        response.put("totalAlerts", alerts.size());

        return response;
    }

    private String getMonthName(int month) {
        String[] months = {"Jan", "Fév", "Mar", "Avr", "Mai", "Jun",
                "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"};
        return months[month - 1];
    }
}