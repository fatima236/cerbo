package com.example.cerbo.controller;

import com.example.cerbo.dto.ProjectDTO;
import com.example.cerbo.dto.meeting.*;
import com.example.cerbo.entity.*;
import com.example.cerbo.service.MeetingService;
import com.example.cerbo.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur unifié pour toutes les opérations liées aux réunions
 *
 * Ce contrôleur suit le principe de cohérence architecturale :
 * - Toutes les réponses utilisent des DTOs (jamais d'entités directes)
 * - Gestion d'erreurs centralisée et cohérente
 * - Logging approprié pour le debugging
 * - Validation des autorisations et des données
 * - Séparation claire entre logique métier (service) et présentation (contrôleur)
 *
 * Organisation des endpoints :
 * - /api/meeting/* : CRUD des réunions
 * - /api/meeting/{id}/agenda/* : Gestion de l'ordre du jour
 * - /api/meeting/{id}/attendees/* : Gestion des participants
 * - /api/meeting/{id}/attendance/* : Gestion des présences
 */
@RestController
@RequestMapping("/api/meeting")
@RequiredArgsConstructor
@Slf4j
public class MeetingController {

    private final MeetingService meetingService;
    private final UserRepository userRepository;

    // ========================================
    // GESTION CRUD DES RÉUNIONS
    // ========================================

    /**
     * Récupère la liste des réunions pour une année donnée
     *
     * Endpoint optimisé pour les calendriers et listes.
     * Utilise la version simplifiée des DTOs pour de meilleures performances.
     *
     * @param year Année des réunions à récupérer
     * @param month Mois spécifique (optionnel)
     * @return Liste des réunions sous forme de DTOs simplifiés
     */
    @GetMapping
    public ResponseEntity<List<MeetingDTO>> getMeetingsByYear(
            @RequestParam int year,
            @RequestParam(required = false) Integer month) {

        try {
            log.info("Récupération des réunions pour l'année {} (mois: {})", year, month);

            List<Meeting> meetings;
            if (month != null) {
                meetings = meetingService.getMeetingsByYearAndMonth(year, month);
            } else {
                meetings = meetingService.getMeetingsByYear(year);
            }

            // Conversion vers DTOs simplifiés pour optimiser les performances
            List<MeetingDTO> meetingDTOs = meetings.stream()
                    .map(MeetingDTO::createSimple)
                    .filter(Objects::nonNull) // Filtrer les conversions échouées
                    .collect(Collectors.toList());

            log.info("Récupération réussie de {} réunions (DTOs simples)", meetingDTOs.size());
            return ResponseEntity.ok(meetingDTOs);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des réunions pour l'année {}: {}",
                    year, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList()); // Retourner une liste vide plutôt que null
        }
    }

    /**
     * Récupère les détails complets d'une réunion par son ID
     *
     * Endpoint pour les pages de détail. Utilise la version complète des DTOs
     * qui inclut toutes les relations (participants, présences, ordre du jour).
     *
     * @param id ID de la réunion
     * @return Détails complets de la réunion sous forme de DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<MeetingDTO> getMeetingById(@PathVariable Long id) {
        try {
            log.info("Récupération des détails complets de la réunion ID: {}", id);

            Meeting meeting = meetingService.getMeetingById(id);
            if (meeting == null) {
                log.warn("Réunion non trouvée avec l'ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            // Conversion vers DTO complet pour les détails
            MeetingDTO meetingDTO = MeetingDTO.createComplete(meeting);

            if (meetingDTO == null) {
                log.error("Erreur lors de la conversion de la réunion ID: {}", id);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            log.info("Récupération réussie de la réunion complète ID: {} avec {} projets, {} participants",
                    id, meetingDTO.getAgendaItemsCount(), meetingDTO.getParticipantsCount());

            return ResponseEntity.ok(meetingDTO);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la réunion ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Met à jour une réunion existante
     *
     * Gère la validation business, notamment pour les dates passées.
     * Utilise le DTO de requête pour la validation et les règles métier.
     *
     * @param id ID de la réunion à mettre à jour
     * @param meetingRequest Données de mise à jour
     * @return Réponse standardisée avec la réunion mise à jour
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateMeeting(
            @PathVariable Long id,
            @Valid @RequestBody MeetingRequestDTO meetingRequest) {

        try {
            log.info("🔄 Mise à jour de la réunion ID: {} avec les données: {}", id, meetingRequest.getSummary());

            // Validation de l'ID
            if (id == null || id <= 0) {
                log.warn("❌ ID de réunion invalide: {}", id);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("ID de réunion invalide", false, null));
            }

            // Vérification de l'existence de la réunion
            Meeting existingMeeting = meetingService.getMeetingById(id);
            if (existingMeeting == null) {
                log.warn("❌ Tentative de mise à jour d'une réunion inexistante: {}", id);
                return ResponseEntity.notFound().build();
            }

            // Application des valeurs par défaut
            meetingRequest.applyDefaults();

            // Validation du DTO
            if (!meetingRequest.isValid()) {
                log.warn("❌ Données de requête invalides: {}", meetingRequest);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Données de requête invalides", false,
                                "Vérifiez que tous les champs requis sont remplis"));
            }

            // Validation business : date dans le passé
            if (meetingRequest.isInPast() && !meetingRequest.shouldForceUpdate()) {
                log.warn("⚠️ Tentative de modification d'une réunion avec date passée sans force: {}",
                        meetingRequest.getDate());

                return ResponseEntity.badRequest()
                        .body(createErrorResponse(
                                "Impossible de modifier une réunion dont la date est passée",
                                true, // canForce
                                "Utilisez le paramètre 'force: true' pour forcer la modification"
                        ));
            }

            // Validation business : date trop ancienne ou future
            if (meetingRequest.isDateTooOld()) {
                log.warn("⚠️ Date trop ancienne (plus d'1 an): {}", meetingRequest.getDate());
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("La date ne peut pas être antérieure à 1 an", false, null));
            }

            if (meetingRequest.isDateTooFuture()) {
                log.warn("⚠️ Date trop future (plus de 2 ans): {}", meetingRequest.getDate());
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("La date ne peut pas être postérieure à 2 ans", false, null));
            }

            // Application des modifications
            log.info("✏️ Application des modifications...");
            existingMeeting.setDate(meetingRequest.getDate());
            existingMeeting.setTime(meetingRequest.getTime());
            existingMeeting.setStatus(meetingRequest.getStatus());
            existingMeeting.setMonth(meetingRequest.getMonth());
            existingMeeting.setYear(meetingRequest.getYear());

            // Sauvegarde via le service
            Meeting updatedMeeting = meetingService.saveMeeting(existingMeeting);

            // Conversion vers DTO simple pour la réponse
            MeetingDTO responseDTO = MeetingDTO.createSimple(updatedMeeting);

            if (responseDTO == null) {
                log.error("❌ Erreur lors de la conversion de la réunion mise à jour");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Erreur lors de la conversion des données", false, null));
            }

            log.info("✅ Réunion ID: {} mise à jour avec succès", id);

            Map<String, Object> response = createSuccessResponse("Réunion mise à jour avec succès", responseDTO);
            response.put("meeting", responseDTO); // Pour compatibilité avec le frontend

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("❌ Erreur de validation lors de la mise à jour de la réunion ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage(), false, null));
        } catch (Exception e) {
            log.error("❌ Erreur technique lors de la mise à jour de la réunion ID {}: {}",
                    id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur", false,
                            "Contactez l'administrateur si le problème persiste"));
        }
    }

    /**
     * Génère automatiquement le planning annuel des réunions
     *
     * Crée un planning prédéfini de 11 réunions pour l'année spécifiée.
     * ⚠️ ATTENTION : Supprime les réunions existantes pour cette année !
     *
     * @param year Année pour laquelle générer le planning
     * @return Réponse avec la liste des réunions créées
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> generateMeetings(@RequestParam int year) {
        try {
            log.info("Génération du planning automatique pour l'année: {}", year);

            // Validation de l'année
            if (year < 2020 || year > 2030) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Année invalide. Doit être entre 2020 et 2030", false, null));
            }

            List<Meeting> generatedMeetings = meetingService.generateMeetings(year);
            List<MeetingDTO> meetingDTOs = generatedMeetings.stream()
                    .map(MeetingDTO::createSimple)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("Planning généré avec succès: {} réunions créées pour l'année {}",
                    meetingDTOs.size(), year);

            Map<String, Object> response = createSuccessResponse(
                    "Planning généré avec succès",
                    null
            );
            response.put("meetings", meetingDTOs);
            response.put("count", meetingDTOs.size());
            response.put("year", year);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du planning pour l'année {}: {}",
                    year, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la génération du planning", false, null));
        }
    }

    // ========================================
    // GESTION DE L'ORDRE DU JOUR
    // ========================================

    /**
     * Récupère l'ordre du jour d'une réunion
     *
     * Retourne la liste des projets dans l'ordre défini, sous forme de DTOs spécialisés.
     *
     * @param meetingId ID de la réunion
     * @return Liste des projets de l'ordre du jour
     */
    @GetMapping("/{meetingId}/agenda")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MeetingProjectDTO>> getAgenda(@PathVariable Long meetingId) {
        try {
            log.info("Récupération de l'ordre du jour pour la réunion ID: {}", meetingId);

            // Récupération via le service unifié
            Meeting meeting = meetingService.getMeetingById(meetingId);
            if (meeting == null) {
                return ResponseEntity.notFound().build();
            }

            // Conversion vers DTOs spécialisés pour l'ordre du jour
            List<MeetingProjectDTO> agendaDTOs = MeetingProjectDTO.fromEntityList(meeting.getAgendaItems());

            log.info("Ordre du jour récupéré avec succès: {} projets pour la réunion {}",
                    agendaDTOs.size(), meetingId);
            return ResponseEntity.ok(agendaDTOs);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'ordre du jour pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Ajoute un projet à l'ordre du jour
     *
     * @param meetingId ID de la réunion
     * @param projectId ID du projet à ajouter
     * @return Réponse de confirmation
     */
    @PostMapping("/{meetingId}/agenda/projects/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> addProjectToAgenda(
            @PathVariable Long meetingId,
            @PathVariable Long projectId) {
        try {
            log.info("Ajout du projet {} à l'ordre du jour de la réunion {}", projectId, meetingId);

            meetingService.addProjectToAgenda(meetingId, projectId);

            return ResponseEntity.ok(createSuccessResponse("Projet ajouté à l'ordre du jour", null));
        } catch (IllegalArgumentException e) {
            log.warn("Erreur business lors de l'ajout du projet {} à la réunion {}: {}",
                    projectId, meetingId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage(), false, null));
        } catch (Exception e) {
            log.error("Erreur technique lors de l'ajout du projet {} à la réunion {}: {}",
                    projectId, meetingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur", false, null));
        }
    }

    /**
     * Supprime un projet de l'ordre du jour
     *
     * @param meetingId ID de la réunion
     * @param projectId ID du projet à supprimer
     * @return Réponse de confirmation
     */
    @DeleteMapping("/{meetingId}/agenda/projects/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> removeProjectFromAgenda(
            @PathVariable Long meetingId,
            @PathVariable Long projectId) {
        try {
            log.info("Suppression du projet {} de l'ordre du jour de la réunion {}", projectId, meetingId);

            meetingService.removeProjectFromAgenda(meetingId, projectId);

            return ResponseEntity.ok(createSuccessResponse("Projet retiré de l'ordre du jour", null));
        } catch (IllegalArgumentException e) {
            log.warn("Erreur business lors de la suppression du projet {} de la réunion {}: {}",
                    projectId, meetingId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage(), false, null));
        } catch (Exception e) {
            log.error("Erreur technique lors de la suppression du projet {} de la réunion {}: {}",
                    projectId, meetingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur", false, null));
        }
    }

    /**
     * Réorganise l'ordre du jour d'une réunion
     *
     * @param meetingId ID de la réunion
     * @param projectIds Liste ordonnée des IDs de projets
     * @return Réponse de confirmation
     */
    @PutMapping("/{meetingId}/agenda/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> reorderAgenda(
            @PathVariable Long meetingId,
            @RequestBody List<Long> projectIds) {
        try {
            log.info("Réorganisation de l'ordre du jour pour la réunion {}: {}", meetingId, projectIds);

            if (projectIds == null || projectIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("La liste des projets ne peut pas être vide", false, null));
            }

            meetingService.reorderAgenda(meetingId, projectIds);

            return ResponseEntity.ok(createSuccessResponse("Ordre du jour réorganisé", null));
        } catch (IllegalArgumentException e) {
            log.warn("Erreur business lors de la réorganisation de la réunion {}: {}", meetingId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage(), false, null));
        } catch (Exception e) {
            log.error("Erreur technique lors de la réorganisation de la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur", false, null));
        }
    }

    // ========================================
    // GESTION DES PARTICIPANTS
    // ========================================

    /**
     * Récupère la liste des participants d'une réunion
     *
     * @param meetingId ID de la réunion
     * @return Liste des participants sous forme de DTOs
     */
    @GetMapping("/{meetingId}/attendees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MeetingParticipantDTO>> getMeetingAttendees(@PathVariable Long meetingId) {
        try {
            log.info("Récupération des participants pour la réunion ID: {}", meetingId);

            List<MeetingAttendee> attendees = meetingService.getMeetingAttendees(meetingId);
            List<MeetingParticipantDTO> participantDTOs = MeetingParticipantDTO.fromEntityList(attendees);

            log.info("Participants récupérés avec succès: {} participants pour la réunion {}",
                    participantDTOs.size(), meetingId);
            return ResponseEntity.ok(participantDTOs);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des participants pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Ajoute un participant à une réunion
     *
     * @param meetingId ID de la réunion
     * @param userId ID de l'utilisateur à ajouter
     * @return Réponse de confirmation
     */
    @PostMapping("/{meetingId}/attendees/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> addAttendee(
            @PathVariable Long meetingId,
            @PathVariable Long userId) {
        try {
            log.info("Ajout du participant {} à la réunion {}", userId, meetingId);

            meetingService.addAttendeeManually(meetingId, userId);

            return ResponseEntity.ok(createSuccessResponse("Évaluateur ajouté à la réunion", null));
        } catch (IllegalArgumentException e) {
            log.warn("Erreur business lors de l'ajout du participant {} à la réunion {}: {}",
                    userId, meetingId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage(), false, null));
        } catch (Exception e) {
            log.error("Erreur technique lors de l'ajout du participant {} à la réunion {}: {}",
                    userId, meetingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur", false, null));
        }
    }

    /**
     * Supprime un participant d'une réunion
     *
     * @param meetingId ID de la réunion
     * @param userId ID de l'utilisateur à supprimer
     * @return Réponse de confirmation
     */
    @DeleteMapping("/{meetingId}/attendees/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> removeAttendee(
            @PathVariable Long meetingId,
            @PathVariable Long userId) {
        try {
            log.info("Suppression du participant {} de la réunion {}", userId, meetingId);

            meetingService.removeAttendee(meetingId, userId);

            return ResponseEntity.ok(createSuccessResponse("Évaluateur retiré de la réunion", null));
        } catch (IllegalArgumentException e) {
            log.warn("Erreur business lors de la suppression du participant {} de la réunion {}: {}",
                    userId, meetingId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage(), false, null));
        } catch (Exception e) {
            log.error("Erreur technique lors de la suppression du participant {} de la réunion {}: {}",
                    userId, meetingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur", false, null));
        }
    }

    /**
     * Met à jour les participants automatiquement depuis l'ordre du jour
     *
     * @param meetingId ID de la réunion
     * @return Réponse de confirmation
     */
    @PostMapping("/{meetingId}/attendees/update-from-agenda")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateAttendeesFromAgenda(@PathVariable Long meetingId) {
        try {
            log.info("Mise à jour automatique des participants depuis l'ordre du jour pour la réunion {}", meetingId);

            meetingService.updateAttendeesFromAgenda(meetingId);

            return ResponseEntity.ok(createSuccessResponse(
                    "Liste des évaluateurs mise à jour en fonction de l'ordre du jour", null));
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour automatique des participants pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur", false, null));
        }
    }

    // ========================================
    // GESTION DES PRÉSENCES
    // ========================================

    /**
     * Récupère les présences d'une réunion
     *
     * @param meetingId ID de la réunion
     * @return Liste des présences sous forme de DTOs
     */
    @GetMapping("/{meetingId}/attendance")
    public ResponseEntity<List<MeetingAttendanceDTO>> getMeetingAttendances(@PathVariable Long meetingId) {
        try {
            log.info("Récupération des présences pour la réunion ID: {}", meetingId);

            List<MeetingAttendance> attendances = meetingService.getAttendancesForMeeting(meetingId);
            List<MeetingAttendanceDTO> attendanceDTOs = MeetingAttendanceDTO.fromEntityList(attendances);

            log.info("Présences récupérées avec succès: {} présences pour la réunion {}",
                    attendanceDTOs.size(), meetingId);
            return ResponseEntity.ok(attendanceDTOs);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des présences pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Marque la présence/absence d'un évaluateur
     *
     * @param meetingId ID de la réunion
     * @param evaluatorId ID de l'évaluateur
     * @param present Présent ou absent
     * @param justification Justification d'absence (optionnelle)
     * @return Données de présence mises à jour
     */
    @PostMapping("/{meetingId}/attendance")
    public ResponseEntity<MeetingAttendanceDTO> markAttendance(
            @PathVariable Long meetingId,
            @RequestParam Long evaluatorId,
            @RequestParam boolean present,
            @RequestParam(required = false) String justification) {
        try {
            log.info("Marquage de présence pour la réunion {} - évaluateur {} - présent: {}",
                    meetingId, evaluatorId, present);

            MeetingAttendance attendance = meetingService.markAttendance(meetingId, evaluatorId, present, justification);
            MeetingAttendanceDTO attendanceDTO = MeetingAttendanceDTO.fromEntity(attendance);

            if (attendanceDTO == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            return ResponseEntity.ok(attendanceDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Erreur business lors du marquage de présence pour la réunion {} - évaluateur {}: {}",
                    meetingId, evaluatorId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur technique lors du marquage de présence pour la réunion {} - évaluateur {}: {}",
                    meetingId, evaluatorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Initialise les présences à partir des invitations
     *
     * @param meetingId ID de la réunion
     * @return Réponse avec le nombre d'entrées créées
     */
    @PostMapping("/{meetingId}/attendance/initialize-from-invitations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> initializeAttendanceFromInvitations(@PathVariable Long meetingId) {
        try {
            log.info("Initialisation des présences depuis les invitations pour la réunion {}", meetingId);

            int createdCount = meetingService.initializeAttendanceFromInvitations(meetingId);

            Map<String, Object> response = createSuccessResponse("Présences initialisées avec succès", null);
            response.put("createdCount", createdCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation des présences pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de l'initialisation des présences", false, e.getMessage()));
        }
    }

    // ========================================
    // UTILITAIRES ET STATISTIQUES
    // ========================================

    /**
     * Récupère tous les évaluateurs disponibles
     *
     * @return Liste des évaluateurs sous forme de DTOs simplifiés
     */
    @GetMapping("/evaluators")
    public ResponseEntity<List<Map<String, Object>>> getAllEvaluators() {
        try {
            log.info("Récupération de tous les évaluateurs");

            List<User> evaluators = meetingService.getAllEvaluators();

            List<Map<String, Object>> response = evaluators.stream()
                    .map(this::convertEvaluatorToSimpleMap)
                    .collect(Collectors.toList());

            log.info("Évaluateurs récupérés avec succès: {} évaluateurs trouvés", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des évaluateurs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Récupère les statistiques des évaluateurs pour une année
     *
     * @param year Année pour les statistiques
     * @return Statistiques de présence des évaluateurs
     */
    @GetMapping("/evaluators/stats")
    public ResponseEntity<List<Map<String, Object>>> getEvaluatorsStats(@RequestParam int year) {
        try {
            log.info("Récupération des statistiques des évaluateurs pour l'année {}", year);

            List<User> evaluators = userRepository.findByRole("EVALUATEUR");
            final int TOTAL_MEETINGS = 11; // Configuration : 11 réunions annuelles

            List<Map<String, Object>> stats = evaluators.stream()
                    .map(evaluator -> {
                        int presences = meetingService.countAnnualPresences(evaluator.getId(), year);
                        int unjustifiedAbsences = meetingService.countAnnualUnjustifiedAbsences(evaluator.getId(), year);

                        Map<String, Object> evaluatorStats = new HashMap<>();
                        evaluatorStats.put("evaluatorId", evaluator.getId());
                        evaluatorStats.put("nom", evaluator.getNom());
                        evaluatorStats.put("prenom", evaluator.getPrenom());
                        evaluatorStats.put("email", evaluator.getEmail());
                        evaluatorStats.put("presenceCount", presences);
                        evaluatorStats.put("unjustifiedAbsences", unjustifiedAbsences);
                        evaluatorStats.put("totalMeetings", TOTAL_MEETINGS);

                        // Calcul du taux de présence
                        double attendanceRate = TOTAL_MEETINGS > 0 ? (presences * 100.0) / TOTAL_MEETINGS : 0;
                        evaluatorStats.put("attendanceRate", Math.round(attendanceRate));

                        return evaluatorStats;
                    })
                    .collect(Collectors.toList());

            log.info("Statistiques calculées pour {} évaluateurs (année {})", stats.size(), year);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques pour l'année {}: {}",
                    year, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Récupère les statistiques complètes d'une réunion
     *
     * @param meetingId ID de la réunion
     * @return Statistiques détaillées de la réunion
     */
    @GetMapping("/{meetingId}/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getMeetingStatistics(@PathVariable Long meetingId) {
        try {
            log.info("Récupération des statistiques pour la réunion ID: {}", meetingId);

            Map<String, Object> statistics = meetingService.getMeetingStatistics(meetingId);

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Impossible de calculer les statistiques", false, null));
        }
    }

    /**
     * Recherche de réunions avec critères multiples
     *
     * @param status Statut des réunions (optionnel)
     * @param fromDate Date de début (optionnel)
     * @param toDate Date de fin (optionnel)
     * @param searchTerm Terme de recherche (optionnel)
     * @return Liste des réunions correspondant aux critères
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MeetingDTO>> searchMeetings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String searchTerm) {
        try {
            log.info("Recherche de réunions - Statut: {}, Du: {}, Au: {}, Terme: {}",
                    status, fromDate, toDate, searchTerm);

            List<Meeting> meetings = meetingService.searchMeetings(status, fromDate, toDate, searchTerm);
            List<MeetingDTO> meetingDTOs = meetings.stream()
                    .map(MeetingDTO::createSimple)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("Recherche terminée: {} réunions trouvées", meetingDTOs.size());
            return ResponseEntity.ok(meetingDTOs);
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de réunions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Téléchargement du planning PDF
     */
    @GetMapping("/download-pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadMeetingsPdf(@RequestParam int year) {
        try {
            log.info("🔄 Demande de téléchargement PDF pour l'année: {}", year);

            // Validation
            if (year < 2020 || year > 2030) {
                log.warn("❌ Année invalide: {}", year);
                return ResponseEntity.badRequest().build();
            }

            // Génération du PDF via le service
            byte[] pdfBytes = meetingService.generateMeetingsPdf(year);
            // OU si c'est dans MeetingAttendanceService :
            // byte[] pdfBytes = meetingAttendanceService.generateMeetingsPdf(year);

            // Headers HTTP pour le téléchargement
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "planning_reunions_" + year + ".pdf");
            headers.setContentLength(pdfBytes.length);

            log.info("✅ PDF généré pour l'année {} - {} bytes", year, pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("❌ Erreur PDF pour l'année {}: {}", year, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ========================================

    /**
     * Convertit un User (évaluateur) en Map simplifiée pour la réponse JSON
     */
    private Map<String, Object> convertEvaluatorToSimpleMap(User evaluator) {
        Map<String, Object> map = new HashMap<>();

        try {
            map.put("id", evaluator.getId());
            map.put("email", evaluator.getEmail());
            map.put("firstName", evaluator.getPrenom());
            map.put("lastName", evaluator.getNom());

            // Construction du nom complet pour l'affichage
            String fullName = buildUserFullName(evaluator);
            map.put("fullName", fullName);
        } catch (Exception e) {
            log.error("Erreur lors de la conversion de User (évaluateur) ID {}: {}",
                    evaluator.getId(), e.getMessage());
            // Valeurs par défaut en cas d'erreur
            map.put("id", evaluator.getId());
            map.put("fullName", "Nom indisponible");
            map.put("email", "email.indisponible@example.com");
        }

        return map;
    }

    /**
     * Construit le nom complet d'un utilisateur de manière sécurisée
     */
    private String buildUserFullName(User user) {
        if (user == null) return "Utilisateur inconnu";

        StringBuilder fullName = new StringBuilder();

        if (user.getCivilite() != null && !user.getCivilite().trim().isEmpty()) {
            fullName.append(user.getCivilite().trim()).append(" ");
        }

        if (user.getPrenom() != null && !user.getPrenom().trim().isEmpty()) {
            fullName.append(user.getPrenom().trim()).append(" ");
        }

        if (user.getNom() != null && !user.getNom().trim().isEmpty()) {
            fullName.append(user.getNom().trim());
        }

        String result = fullName.toString().trim();
        return result.isEmpty() ? "Nom non défini" : result;
    }

    /**
     * Crée une réponse d'erreur standardisée
     */
    private Map<String, Object> createErrorResponse(String message, boolean canForce, String details) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("canForce", canForce);
        response.put("timestamp", System.currentTimeMillis());

        if (details != null && !details.trim().isEmpty()) {
            response.put("details", details);
        }

        return response;
    }

    /**
     * Crée une réponse de succès standardisée
     */
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());

        if (data != null) {
            response.put("data", data);
        }

        return response;
    }

    @GetMapping("/evaluator/{evaluatorId}")
    public ResponseEntity<List<MeetingWithProjectsDTO>> getMeetingsForEvaluator(
            @PathVariable Long evaluatorId,
            @RequestParam(required = false) Integer year) {
        try {
            log.info("Récupération des réunions pour l'évaluateur ID: {}", evaluatorId);

            List<Meeting> meetings = meetingService.getMeetingsForEvaluator(evaluatorId, year);

            List<MeetingWithProjectsDTO> dtos = meetings.stream()
                    .map(this::convertToMeetingWithProjectsDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des réunions pour l'évaluateur {}: {}",
                    evaluatorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private MeetingWithProjectsDTO convertToMeetingWithProjectsDTO(Meeting meeting) {
        MeetingWithProjectsDTO dto = new MeetingWithProjectsDTO();
        dto.setId(meeting.getId());
        dto.setDate(meeting.getDate());
        dto.setTime(meeting.getTime());
        dto.setStatus(meeting.getStatus());

        // Convert to MeetingWithProjectsDTO.ProjectDTO
        List<MeetingWithProjectsDTO.ProjectDTO> projects = meeting.getAgendaItems().stream()
                .filter(mp -> mp.getProject() != null)
                .map(mp -> {
                    MeetingWithProjectsDTO.ProjectDTO projectDto = new MeetingWithProjectsDTO.ProjectDTO();
                    projectDto.setId(mp.getProject().getId());
                    projectDto.setTitle(mp.getProject().getTitle());
                    projectDto.setReference(mp.getProject().getReference());
                    return projectDto;
                })
                .collect(Collectors.toList());

        dto.setProjects(projects);
        return dto;
    }}