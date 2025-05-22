package com.example.cerbo.controller;

import com.example.cerbo.entity.Project;
import com.example.cerbo.service.MeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meeting")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class MeetingAgendaController {

    private final MeetingService meetingService;

    @GetMapping("/{meetingId}/agenda")
    public ResponseEntity<List<Project>> getAgenda(@PathVariable Long meetingId) {
        try {
            List<Project> projects = meetingService.getAgendaProjects(meetingId);
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'ordre du jour", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{meetingId}/agenda/projects/{projectId}")
    public ResponseEntity<?> addProjectToAgenda(
            @PathVariable Long meetingId,
            @PathVariable Long projectId) {
        try {
            meetingService.addProjectToAgenda(meetingId, projectId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Projet ajouté à l'ordre du jour"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout du projet à l'ordre du jour", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur interne du serveur"
            ));
        }
    }

    @DeleteMapping("/{meetingId}/agenda/projects/{projectId}")
    public ResponseEntity<?> removeProjectFromAgenda(
            @PathVariable Long meetingId,
            @PathVariable Long projectId) {
        try {
            meetingService.removeProjectFromAgenda(meetingId, projectId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Projet retiré de l'ordre du jour"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du projet de l'ordre du jour", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur interne du serveur"
            ));
        }
    }

    @PutMapping("/{meetingId}/agenda/reorder")
    public ResponseEntity<?> reorderAgenda(
            @PathVariable Long meetingId,
            @RequestBody List<Long> projectIds) {
        try {
            meetingService.reorderAgenda(meetingId, projectIds);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Ordre du jour réorganisé"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Erreur lors de la réorganisation de l'ordre du jour", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur interne du serveur"
            ));
        }
    }

    @GetMapping("/{meetingId}/agenda/count")
    public ResponseEntity<Map<String, Object>> getAgendaCount(@PathVariable Long meetingId) {
        try {
            long count = meetingService.getAgendaProjects(meetingId).size();
            return ResponseEntity.ok(Map.of(
                    "count", count,
                    "meetingId", meetingId
            ));
        } catch (Exception e) {
            log.error("Erreur lors du comptage des éléments de l'ordre du jour", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}