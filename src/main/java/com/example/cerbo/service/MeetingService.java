package com.example.cerbo.service;

import com.example.cerbo.entity.*;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Image;
import java.io.IOException;
import java.time.LocalDateTime;
import com.example.cerbo.repository.*;
import com.example.cerbo.annotation.Loggable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Service unifié pour toutes les opérations liées aux réunions
 *
 * Ce service centralise toute la logique métier des réunions et suit le principe
 * de responsabilité unique. Chaque méthode a un objectif clair et bien défini.
 *
 * Responsabilités principales :
 * - Gestion du CRUD des réunions
 * - Génération automatique du planning annuel
 * - Gestion de l'ordre du jour
 * - Gestion des participants et présences
 * - Calculs de statistiques
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {

    // Injection des repositories nécessaires
    private final MeetingRepository meetingRepository;
    private final MeetingProjectRepository meetingProjectRepository;
    private final MeetingAttendeeRepository meetingAttendeeRepository;
    private final MeetingAttendanceRepository attendanceRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ========================================
    // GESTION CRUD DES RÉUNIONS
    // ========================================

    /**
     * Récupère toutes les réunions pour une année donnée
     *
     * Cette méthode est optimisée pour les listes et calendriers.
     * Elle ne charge pas les relations complexes pour éviter les problèmes de performance.
     */
    @Loggable(actionType = "READ", entityType = "MEETING")
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByYear(int year) {
        try {
            log.debug("Récupération des réunions pour l'année {}", year);
            List<Meeting> meetings = meetingRepository.findByYear(year);
            log.debug("Trouvé {} réunions pour l'année {}", meetings.size(), year);
            return meetings;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des réunions pour l'année {}: {}", year, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer les réunions pour l'année " + year, e);
        }
    }

    /**
     * Récupère les réunions pour une année et un mois spécifiques
     *
     * Utile pour les vues mensuelles ou les filtres temporels spécifiques.
     */
    @Loggable(actionType = "READ", entityType = "MEETING")
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByYearAndMonth(int year, int month) {
        try {
            log.debug("Récupération des réunions pour l'année {} et le mois {}", year, month);

            List<Meeting> allMeetings = meetingRepository.findByYear(year);
            List<Meeting> filteredMeetings = allMeetings.stream()
                    .filter(meeting -> {
                        if (meeting.getDate() == null) {
                            log.warn("Réunion ID {} n'a pas de date définie", meeting.getId());
                            return false;
                        }
                        return meeting.getDate().getMonthValue() == month;
                    })
                    .collect(Collectors.toList());

            log.debug("Trouvé {} réunions pour l'année {} et le mois {}", filteredMeetings.size(), year, month);
            return filteredMeetings;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des réunions pour l'année {} et le mois {}: {}",
                    year, month, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer les réunions pour l'année " + year + " et le mois " + month, e);
        }
    }

    /**
     * Récupère une réunion par son ID avec toutes ses relations
     *
     * Cette méthode charge toutes les relations pour les vues de détail.
     * Utilisée principalement pour les pages de détail de réunion.
     */
    @Loggable(actionType = "READ", entityType = "MEETING")
    @Transactional(readOnly = true)
    public Meeting getMeetingById(Long id) {
        try {
            log.debug("Récupération de la réunion complète ID: {}", id);
            Optional<Meeting> meeting = meetingRepository.findById(id);

            if (meeting.isEmpty()) {
                log.warn("Aucune réunion trouvée avec l'ID: {}", id);
                return null;
            }

            Meeting foundMeeting = meeting.get();
            log.debug("Réunion trouvée ID: {} avec {} projets, {} participants, {} présences",
                    id,
                    foundMeeting.getAgendaItems().size(),
                    foundMeeting.getAttendees().size(),
                    foundMeeting.getAttendances().size());

            return foundMeeting;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la réunion ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer la réunion avec l'ID " + id, e);
        }
    }

    /**
     * Sauvegarde une réunion (création ou mise à jour)
     *
     * Cette méthode gère automatiquement la logique métier :
     * - Génération automatique de référence pour les nouvelles réunions
     * - Mise à jour automatique du statut pour les réunions passées
     * - Validation des données avant sauvegarde
     */
    @Loggable(actionType = "SAVE", entityType = "MEETING")
    @Transactional
    public Meeting saveMeeting(Meeting meeting) {
        try {
            log.info("Sauvegarde de la réunion: {}", meeting.getId() != null ? "mise à jour ID " + meeting.getId() : "nouvelle création");

            // Validation des données de base
            if (meeting.getDate() == null || meeting.getTime() == null) {
                throw new IllegalArgumentException("La date et l'heure sont obligatoires");
            }

            // Logique métier automatique
            applyBusinessRules(meeting);

            Meeting savedMeeting = meetingRepository.save(meeting);
            log.info("Réunion sauvegardée avec succès - ID: {}, Date: {}, Statut: {}",
                    savedMeeting.getId(), savedMeeting.getDate(), savedMeeting.getStatus());

            return savedMeeting;
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde de la réunion: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible de sauvegarder la réunion", e);
        }
    }

    /**
     * Applique les règles métier automatiques à une réunion
     */
    private void applyBusinessRules(Meeting meeting) {
        // Mettre à jour l'année si la date a changé
        if (meeting.getDate() != null) {
            meeting.setYear(meeting.getDate().getYear());
        }

        // Mettre à jour automatiquement le statut pour les réunions passées
        if (isPastMeeting(meeting) && !"Terminée".equals(meeting.getStatus())) {
            log.info("Mise à jour automatique du statut de la réunion {} vers 'Terminée' (date passée)", meeting.getId());
            meeting.setStatus("Terminée");
        }

        // Valeur par défaut pour le statut
        if (meeting.getStatus() == null || meeting.getStatus().trim().isEmpty()) {
            meeting.setStatus("Planifiée");
        }
    }

    /**
     * Vérifie si une réunion est dans le passé
     */
    private boolean isPastMeeting(Meeting meeting) {
        if (meeting.getDate() == null || meeting.getTime() == null) {
            return false;
        }

        LocalDate now = LocalDate.now();
        LocalDate meetingDate = meeting.getDate();

        if (meetingDate.isBefore(now)) {
            return true;
        }

        if (meetingDate.isEqual(now)) {
            LocalTime nowTime = LocalTime.now();
            LocalTime meetingTime = meeting.getTime();
            return meetingTime.isBefore(nowTime);
        }

        return false;
    }

    /**
     * Supprime une réunion et toutes ses relations associées
     */
    @Loggable(actionType = "DELETE", entityType = "MEETING")
    @Transactional
    public void deleteMeeting(Long id) {
        try {
            log.info("Suppression de la réunion ID: {}", id);

            Meeting meeting = getMeetingById(id);
            if (meeting == null) {
                throw new IllegalArgumentException("Aucune réunion trouvée avec l'ID " + id);
            }

            // Les suppressions en cascade sont gérées par les annotations JPA
            meetingRepository.deleteById(id);
            log.info("Réunion ID {} supprimée avec succès", id);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la réunion ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Impossible de supprimer la réunion", e);
        }
    }

    // ========================================
    // GÉNÉRATION AUTOMATIQUE DU PLANNING
    // ========================================

    /**
     * Génère automatiquement le planning annuel des réunions
     *
     * Cette méthode crée un planning prédéfini de 11 réunions par an,
     * en suivant un calendrier optimal pour les activités du comité.
     *
     * ⚠️ ATTENTION : Cette méthode supprime toutes les réunions existantes pour l'année !
     */
    @Loggable(actionType = "CREATE", entityType = "MEETING")
    @Transactional
    public List<Meeting> generateMeetings(int year) {
        try {
            log.info("Génération du planning automatique pour l'année {}", year);

            // Suppression des réunions existantes pour cette année
            log.warn("Suppression de toutes les réunions existantes pour l'année {}", year);
            meetingRepository.deleteByYear(year);

            // Définition du planning optimal (11 réunions par an)
            List<Meeting> plannedMeetings = Arrays.asList(
                    createOptimalMeeting(year, 1, 25, DayOfWeek.THURSDAY, 15, 0),   // Janvier
                    createOptimalMeeting(year, 2, 25, DayOfWeek.TUESDAY, 15, 0),    // Février
                    createOptimalMeeting(year, 3, 26, DayOfWeek.WEDNESDAY, 13, 0),  // Mars
                    createOptimalMeeting(year, 4, 24, DayOfWeek.THURSDAY, 15, 0),   // Avril
                    createOptimalMeeting(year, 5, 26, DayOfWeek.MONDAY, 15, 0),     // Mai
                    createOptimalMeeting(year, 6, 24, DayOfWeek.TUESDAY, 15, 0),    // Juin
                    createOptimalMeeting(year, 7, 30, DayOfWeek.WEDNESDAY, 15, 0),  // Juillet
                    // Pas de réunion en août (congés)
                    createOptimalMeeting(year, 9, 25, DayOfWeek.THURSDAY, 15, 0),   // Septembre
                    createOptimalMeeting(year, 10, 27, DayOfWeek.MONDAY, 15, 0),    // Octobre
                    createOptimalMeeting(year, 11, 25, DayOfWeek.TUESDAY, 15, 0),   // Novembre
                    createOptimalMeeting(year, 12, 20, DayOfWeek.WEDNESDAY, 15, 0)  // Décembre
            );

            // Sauvegarde des réunions avec gestion d'erreurs individuelles
            List<Meeting> savedMeetings = new ArrayList<>();
            for (Meeting meeting : plannedMeetings) {
                try {
                    Meeting savedMeeting = meetingRepository.save(meeting);
                    savedMeetings.add(savedMeeting);
                    log.debug("Réunion créée: {} à {}", savedMeeting.getDate(), savedMeeting.getTime());
                } catch (Exception e) {
                    log.error("Erreur lors de la création d'une réunion pour le {}: {}",
                            meeting.getDate(), e.getMessage());
                    // Continue avec les autres réunions même si une échoue
                }
            }

            log.info("Planning généré avec succès: {} réunions créées pour l'année {}",
                    savedMeetings.size(), year);
            return savedMeetings;
        } catch (Exception e) {
            log.error("Erreur lors de la génération du planning pour l'année {}: {}", year, e.getMessage(), e);
            throw new RuntimeException("Impossible de générer le planning pour l'année " + year, e);
        }
    }

    /**
     * Crée une réunion optimale avec ajustement automatique du jour de la semaine
     */
    private Meeting createOptimalMeeting(int year, int month, int preferredDay, DayOfWeek targetDayOfWeek, int hour, int minute) {
        try {
            // Calcul de la date optimale
            LocalDate targetDate = LocalDate.of(year, month, preferredDay);

            // Ajustement au jour de la semaine souhaité si nécessaire
            if (targetDate.getDayOfWeek() != targetDayOfWeek) {
                targetDate = targetDate.with(TemporalAdjusters.nextOrSame(targetDayOfWeek));

                // Si on dépasse le mois, prendre le précédent
                if (targetDate.getMonthValue() != month) {
                    targetDate = targetDate.with(TemporalAdjusters.previous(targetDayOfWeek));
                }
            }

            Meeting meeting = new Meeting();
            meeting.setDate(targetDate);
            meeting.setTime(LocalTime.of(hour, minute));
            meeting.setYear(year);
            meeting.setMonth(getMonthName(month));

            // Statut automatique selon la date
            if (isPastMeeting(meeting)) {
                meeting.setStatus("Terminée");
            } else {
                meeting.setStatus("Planifiée");
            }

            return meeting;
        } catch (Exception e) {
            log.error("Erreur lors de la création d'une réunion pour {}/{}: {}", month, year, e.getMessage());
            throw new RuntimeException("Impossible de créer la réunion", e);
        }
    }

    /**
     * Convertit un numéro de mois en nom français
     */
    private String getMonthName(int month) {
        String[] monthNames = {
                "", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
        };

        if (month >= 1 && month <= 12) {
            return monthNames[month];
        }
        return "Mois inconnu";
    }

    // ========================================
    // GESTION DE L'ORDRE DU JOUR
    // ========================================

    /**
     * Récupère tous les projets à l'ordre du jour d'une réunion
     *
     * Les projets sont retournés dans l'ordre défini par orderIndex.
     */
    @Transactional(readOnly = true)
    public List<Project> getAgendaProjects(Long meetingId) {
        try {
            log.debug("Récupération de l'ordre du jour pour la réunion ID: {}", meetingId);

            List<MeetingProject> meetingProjects = meetingProjectRepository.findByMeetingIdOrderByOrderIndex(meetingId);
            List<Project> projects = meetingProjects.stream()
                    .map(MeetingProject::getProject)
                    .filter(Objects::nonNull) // Filtrer les projets null
                    .collect(Collectors.toList());

            log.debug("Trouvé {} projets dans l'ordre du jour de la réunion {}", projects.size(), meetingId);
            return projects;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'ordre du jour pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer l'ordre du jour", e);
        }
    }

    /**
     * Ajoute un projet à l'ordre du jour d'une réunion
     *
     * Le projet est automatiquement placé à la fin de l'ordre du jour.
     */
    @Transactional
    public void addProjectToAgenda(Long meetingId, Long projectId) {
        try {
            log.info("Ajout du projet {} à l'ordre du jour de la réunion {}", projectId, meetingId);

            // Vérification que le projet n'est pas déjà dans l'ordre du jour
            if (meetingProjectRepository.existsByMeetingIdAndProjectId(meetingId, projectId)) {
                throw new IllegalArgumentException("Le projet est déjà dans l'ordre du jour de cette réunion");
            }

            // Récupération des entités
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new IllegalArgumentException("Réunion non trouvée avec l'ID: " + meetingId));

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new IllegalArgumentException("Projet non trouvé avec l'ID: " + projectId));

            // Calcul de l'index de position (à la fin)
            Integer nextOrderIndex = meetingProjectRepository.getNextOrderIndex(meetingId);

            // Création de la relation
            MeetingProject meetingProject = new MeetingProject();
            meetingProject.setMeeting(meeting);
            meetingProject.setProject(project);
            meetingProject.setOrderIndex(nextOrderIndex);

            meetingProjectRepository.save(meetingProject);
            log.info("Projet {} ajouté à l'ordre du jour de la réunion {} à la position {}",
                    projectId, meetingId, nextOrderIndex);
        } catch (IllegalArgumentException e) {
            // Erreurs business - pas de log d'erreur, juste re-throw
            throw e;
        } catch (Exception e) {
            log.error("Erreur technique lors de l'ajout du projet {} à la réunion {}: {}",
                    projectId, meetingId, e.getMessage(), e);
            throw new RuntimeException("Impossible d'ajouter le projet à l'ordre du jour", e);
        }
    }

    /**
     * Supprime un projet de l'ordre du jour d'une réunion
     *
     * Les indices des autres projets sont automatiquement réorganisés.
     */
    @Transactional
    public void removeProjectFromAgenda(Long meetingId, Long projectId) {
        try {
            log.info("Suppression du projet {} de l'ordre du jour de la réunion {}", projectId, meetingId);

            // Vérification que le projet est bien dans l'ordre du jour
            if (!meetingProjectRepository.existsByMeetingIdAndProjectId(meetingId, projectId)) {
                throw new IllegalArgumentException("Le projet n'est pas dans l'ordre du jour de cette réunion");
            }

            // Suppression
            meetingProjectRepository.deleteByMeetingIdAndProjectId(meetingId, projectId);

            // Réorganisation automatique des indices
            reorganizeAgendaIndices(meetingId);

            log.info("Projet {} supprimé de l'ordre du jour de la réunion {} et indices réorganisés",
                    projectId, meetingId);
        } catch (IllegalArgumentException e) {
            // Erreurs business - pas de log d'erreur, juste re-throw
            throw e;
        } catch (Exception e) {
            log.error("Erreur technique lors de la suppression du projet {} de la réunion {}: {}",
                    projectId, meetingId, e.getMessage(), e);
            throw new RuntimeException("Impossible de supprimer le projet de l'ordre du jour", e);
        }
    }

    /**
     * Réorganise l'ordre du jour d'une réunion selon une nouvelle séquence
     */
    @Transactional
    public void reorderAgenda(Long meetingId, List<Long> projectIds) {
        try {
            log.info("Réorganisation de l'ordre du jour pour la réunion {} : {}", meetingId, projectIds);

            // Récupération de l'ordre du jour actuel
            List<MeetingProject> currentAgenda = meetingProjectRepository.findByMeetingIdOrderByOrderIndex(meetingId);

            // Validation : vérifier que tous les projets fournis sont bien dans l'ordre du jour
            Set<Long> currentProjectIds = currentAgenda.stream()
                    .map(mp -> mp.getProject().getId())
                    .collect(Collectors.toSet());

            for (Long projectId : projectIds) {
                if (!currentProjectIds.contains(projectId)) {
                    throw new IllegalArgumentException("Le projet ID " + projectId + " n'est pas dans l'ordre du jour");
                }
            }

            if (currentProjectIds.size() != projectIds.size()) {
                throw new IllegalArgumentException("Le nombre de projets ne correspond pas à l'ordre du jour actuel");
            }

            // Réorganisation
            for (int i = 0; i < projectIds.size(); i++) {
                Long projectId = projectIds.get(i);
                MeetingProject meetingProject = currentAgenda.stream()
                        .filter(mp -> mp.getProject().getId().equals(projectId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Projet non trouvé dans l'ordre du jour: " + projectId));

                meetingProject.setOrderIndex(i);
                meetingProjectRepository.save(meetingProject);
            }

            log.info("Ordre du jour réorganisé avec succès pour la réunion {}", meetingId);
        } catch (IllegalArgumentException e) {
            // Erreurs business - pas de log d'erreur, juste re-throw
            throw e;
        } catch (Exception e) {
            log.error("Erreur technique lors de la réorganisation de l'ordre du jour pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            throw new RuntimeException("Impossible de réorganiser l'ordre du jour", e);
        }
    }

    /**
     * Méthode utilitaire pour réorganiser les indices après une suppression
     */
    private void reorganizeAgendaIndices(Long meetingId) {
        List<MeetingProject> agenda = meetingProjectRepository.findByMeetingIdOrderByOrderIndex(meetingId);
        for (int i = 0; i < agenda.size(); i++) {
            MeetingProject mp = agenda.get(i);
            mp.setOrderIndex(i);
            meetingProjectRepository.save(mp);
        }
    }

    // ========================================
    // GESTION DES PARTICIPANTS
    // ========================================

    /**
     * Récupère tous les participants d'une réunion
     */
    @Transactional(readOnly = true)
    public List<MeetingAttendee> getMeetingAttendees(Long meetingId) {
        try {
            log.debug("Récupération des participants pour la réunion ID: {}", meetingId);
            List<MeetingAttendee> attendees = meetingAttendeeRepository.findByMeetingId(meetingId);
            log.debug("Trouvé {} participants pour la réunion {}", attendees.size(), meetingId);
            return attendees;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des participants pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer les participants", e);
        }
    }

    /**
     * Ajoute un participant manuellement à une réunion
     */
    @Transactional
    public void addAttendeeManually(Long meetingId, Long userId) {
        try {
            log.info("Ajout manuel du participant {} à la réunion {}", userId, meetingId);

            // Vérifier si déjà invité
            MeetingAttendee existingAttendee = meetingAttendeeRepository.findByMeetingIdAndUserId(meetingId, userId);

            if (existingAttendee != null) {
                if (!existingAttendee.getAddedManually()) {
                    // Transformer l'invitation automatique en manuelle
                    existingAttendee.setAddedManually(true);
                    meetingAttendeeRepository.save(existingAttendee);
                    log.info("Participant {} transformé en invitation manuelle pour la réunion {}", userId, meetingId);
                } else {
                    throw new IllegalArgumentException("L'évaluateur est déjà invité manuellement à cette réunion");
                }
                return;
            }

            // Vérifications des entités
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new IllegalArgumentException("Réunion non trouvée avec l'ID: " + meetingId));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + userId));

            // Vérifier que c'est bien un évaluateur
            if (!user.getRoles().contains("EVALUATEUR")) {
                throw new IllegalArgumentException("L'utilisateur n'est pas un évaluateur");
            }

            // Créer la nouvelle invitation
            MeetingAttendee attendee = new MeetingAttendee();
            attendee.setMeeting(meeting);
            attendee.setUser(user);
            attendee.setAddedManually(true);
            attendee.setRelatedProject(null); // Pas de projet lié pour les invitations manuelles

            meetingAttendeeRepository.save(attendee);
            log.info("Participant {} ajouté manuellement à la réunion {}", userId, meetingId);
        } catch (IllegalArgumentException e) {
            // Erreurs business - pas de log d'erreur, juste re-throw
            throw e;
        } catch (Exception e) {
            log.error("Erreur technique lors de l'ajout manuel du participant {} à la réunion {}: {}",
                    userId, meetingId, e.getMessage(), e);
            throw new RuntimeException("Impossible d'ajouter le participant", e);
        }
    }

    /**
     * Supprime un participant d'une réunion
     */
    @Transactional
    public void removeAttendee(Long meetingId, Long userId) {
        try {
            log.info("Suppression du participant {} de la réunion {}", userId, meetingId);

            MeetingAttendee attendee = meetingAttendeeRepository.findByMeetingIdAndUserId(meetingId, userId);

            if (attendee == null) {
                throw new IllegalArgumentException("L'évaluateur n'est pas invité à cette réunion");
            }

            // Supprimer aussi les présences liées si elles existent
            Optional<MeetingAttendance> attendance = attendanceRepository.findByMeetingIdAndEvaluatorId(meetingId, userId);
            if (attendance.isPresent()) {
                attendanceRepository.delete(attendance.get());
                log.debug("Présence supprimée pour l'évaluateur {} de la réunion {}", userId, meetingId);
            }

            meetingAttendeeRepository.delete(attendee);
            log.info("Participant {} supprimé de la réunion {}", userId, meetingId);
        } catch (IllegalArgumentException e) {
            // Erreurs business - pas de log d'erreur, juste re-throw
            throw e;
        } catch (Exception e) {
            log.error("Erreur technique lors de la suppression du participant {} de la réunion {}: {}",
                    userId, meetingId, e.getMessage(), e);
            throw new RuntimeException("Impossible de supprimer le participant", e);
        }
    }

    /**
     * Met à jour automatiquement les participants en fonction de l'ordre du jour
     */
    @Transactional
    public void updateAttendeesFromAgenda(Long meetingId) {
        try {
            log.info("Mise à jour automatique des participants depuis l'ordre du jour pour la réunion {}", meetingId);

            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new IllegalArgumentException("Réunion non trouvée"));

            // Récupérer tous les projets à l'ordre du jour
            List<Project> agendaProjects = getAgendaProjects(meetingId);

            // Récupérer les participants actuels
            List<MeetingAttendee> currentAttendees = meetingAttendeeRepository.findByMeetingId(meetingId);

            // Collecter tous les évaluateurs pertinents
            Set<Long> relevantEvaluatorIds = new HashSet<>();

            for (Project project : agendaProjects) {
                if (project.getReviewers() != null && !project.getReviewers().isEmpty()) {
                    for (User reviewer : project.getReviewers()) {
                        relevantEvaluatorIds.add(reviewer.getId());
                    }
                }
            }

            // Ajouter les nouveaux évaluateurs automatiquement
            for (Long evaluatorId : relevantEvaluatorIds) {
                MeetingAttendee existingAttendee = meetingAttendeeRepository.findByMeetingIdAndUserId(meetingId, evaluatorId);

                if (existingAttendee == null) {
                    // Trouver le projet lié (prendre le premier si plusieurs)
                    Project relatedProject = agendaProjects.stream()
                            .filter(p -> p.getReviewers().stream().anyMatch(r -> r.getId().equals(evaluatorId)))
                            .findFirst()
                            .orElse(null);

                    User evaluator = userRepository.findById(evaluatorId)
                            .orElseThrow(() -> new RuntimeException("Évaluateur non trouvé"));

                    MeetingAttendee newAttendee = new MeetingAttendee();
                    newAttendee.setMeeting(meeting);
                    newAttendee.setUser(evaluator);
                    newAttendee.setAddedManually(false); // Ajout automatique
                    newAttendee.setRelatedProject(relatedProject);

                    meetingAttendeeRepository.save(newAttendee);
                    log.debug("Évaluateur {} ajouté automatiquement à la réunion {} (projet: {})",
                            evaluatorId, meetingId, relatedProject != null ? relatedProject.getTitle() : "N/A");
                }
            }

            // Supprimer les évaluateurs automatiques qui ne sont plus pertinents
            List<MeetingAttendee> attendeesToRemove = currentAttendees.stream()
                    .filter(a -> !a.getAddedManually()) // Seulement les automatiques
                    .filter(a -> !relevantEvaluatorIds.contains(a.getUser().getId()))
                    .collect(Collectors.toList());

            if (!attendeesToRemove.isEmpty()) {
                meetingAttendeeRepository.deleteAll(attendeesToRemove);
                log.info("Supprimé {} participants automatiques obsolètes de la réunion {}",
                        attendeesToRemove.size(), meetingId);
            }

            log.info("Mise à jour automatique terminée pour la réunion {} : {} évaluateurs pertinents identifiés",
                    meetingId, relevantEvaluatorIds.size());
        } catch (IllegalArgumentException e) {
            // Erreurs business - pas de log d'erreur, juste re-throw
            throw e;
        } catch (Exception e) {
            log.error("Erreur technique lors de la mise à jour automatique des participants pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            throw new RuntimeException("Impossible de mettre à jour les participants", e);
        }
    }

    // ========================================
    // GESTION DES PRÉSENCES
    // ========================================

    /**
     * Récupère toutes les présences pour une réunion donnée
     */
    @Transactional(readOnly = true)
    public List<MeetingAttendance> getAttendancesForMeeting(Long meetingId) {
        try {
            log.debug("Récupération des présences pour la réunion ID: {}", meetingId);
            List<MeetingAttendance> attendances = attendanceRepository.findByMeetingId(meetingId);
            log.debug("Trouvé {} présences pour la réunion {}", attendances.size(), meetingId);
            return attendances;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des présences pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer les présences", e);
        }
    }

    /**
     * Marque la présence ou absence d'un évaluateur pour une réunion
     */
    @Transactional
    public MeetingAttendance markAttendance(Long meetingId, Long evaluatorId, boolean present, String justification) {
        MeetingAttendance attendance = attendanceRepository
                .findByMeetingIdAndEvaluatorId(meetingId, evaluatorId)
                .orElseGet(() -> {
                    MeetingAttendance newAttendance = new MeetingAttendance();
                    newAttendance.setMeeting(meetingRepository.getReferenceById(meetingId));
                    newAttendance.setEvaluator(userRepository.getReferenceById(evaluatorId));
                    return newAttendance;
                });

        // Enregistre l'ancien état dans l'historique avant de modifier
        attendance.recordChange(present, justification, !present && justification != null && !justification.isEmpty());

        return attendanceRepository.save(attendance);
    }
    /**
     * Crée une nouvelle entrée de présence
     */
    private MeetingAttendance createNewAttendance(Long meetingId, Long evaluatorId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Réunion non trouvée avec l'ID: " + meetingId));

        User evaluator = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new RuntimeException("Évaluateur non trouvé avec l'ID: " + evaluatorId));

        // Vérification que l'utilisateur est bien un évaluateur
        if (!evaluator.getRoles().contains("EVALUATEUR")) {
            throw new IllegalArgumentException("L'utilisateur n'est pas un évaluateur");
        }

        MeetingAttendance newAttendance = new MeetingAttendance();
        newAttendance.setMeeting(meeting);
        newAttendance.setEvaluator(evaluator);
        newAttendance.setPresent(false); // Par défaut absent
        newAttendance.setJustified(false);

        log.debug("Création d'une nouvelle entrée de présence pour l'évaluateur {} à la réunion {}", evaluatorId, meetingId);
        return newAttendance;
    }

    /**
     * Initialise les présences à partir des participants invités
     */
    @Transactional
    public int initializeAttendanceFromInvitations(Long meetingId) {
        try {
            log.info("Initialisation des présences depuis les invitations pour la réunion {}", meetingId);

            // Récupérer les participants invités
            List<MeetingAttendee> invitedAttendees = meetingAttendeeRepository.findByMeetingId(meetingId);

            if (invitedAttendees.isEmpty()) {
                log.warn("Aucun participant invité trouvé pour la réunion {}", meetingId);
                return 0;
            }

            // Vérifier quels participants ont déjà une entrée de présence
            List<MeetingAttendance> existingAttendances = attendanceRepository.findByMeetingId(meetingId);
            Set<Long> existingEvaluatorIds = existingAttendances.stream()
                    .map(attendance -> attendance.getEvaluator().getId())
                    .collect(Collectors.toSet());

            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Réunion non trouvée"));

            int createdCount = 0;

            // Créer des entrées de présence pour les nouveaux participants
            for (MeetingAttendee attendee : invitedAttendees) {
                User evaluator = attendee.getUser();

                if (!existingEvaluatorIds.contains(evaluator.getId())) {
                    MeetingAttendance attendance = new MeetingAttendance();
                    attendance.setMeeting(meeting);
                    attendance.setEvaluator(evaluator);
                    attendance.setPresent(false); // Par défaut, absent jusqu'à indication contraire
                    attendance.setJustified(false);
                    attendance.setJustification(null);

                    attendanceRepository.save(attendance);
                    createdCount++;

                    log.debug("Présence initialisée pour l'évaluateur {} ({}) à la réunion {}",
                            evaluator.getId(), evaluator.getEmail(), meetingId);
                }
            }

            log.info("Initialisation terminée : {} nouvelles entrées de présence créées pour la réunion {}",
                    createdCount, meetingId);
            return createdCount;
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation des présences pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            throw new RuntimeException("Impossible d'initialiser les présences", e);
        }
    }

    // ========================================
    // UTILITAIRES ET STATISTIQUES
    // ========================================

    /**
     * Récupère tous les évaluateurs disponibles
     */
    @Transactional(readOnly = true)
    public List<User> getAllEvaluators() {
        try {
            log.debug("Récupération de tous les évaluateurs");
            List<User> evaluators = userRepository.findByRole("EVALUATEUR");
            log.debug("Trouvé {} évaluateurs", evaluators.size());
            return evaluators;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des évaluateurs: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer les évaluateurs", e);
        }
    }

    /**
     * Compte les présences annuelles d'un évaluateur
     */
    @Transactional(readOnly = true)
    public int countAnnualPresences(Long evaluatorId, int year) {
        try {
            return attendanceRepository.countAnnualPresences(evaluatorId, year);
        } catch (Exception e) {
            log.error("Erreur lors du comptage des présences pour l'évaluateur {} en {}: {}",
                    evaluatorId, year, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Compte les absences non justifiées annuelles d'un évaluateur
     */
    @Transactional(readOnly = true)
    public int countAnnualUnjustifiedAbsences(Long evaluatorId, int year) {
        try {
            return attendanceRepository.countAnnualUnjustifiedAbsences(evaluatorId, year);
        } catch (Exception e) {
            log.error("Erreur lors du comptage des absences non justifiées pour l'évaluateur {} en {}: {}",
                    evaluatorId, year, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Calcule des statistiques complètes pour une réunion
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMeetingStatistics(Long meetingId) {
        try {
            log.debug("Calcul des statistiques pour la réunion ID: {}", meetingId);

            Map<String, Object> stats = new HashMap<>();

            // Statistiques de base
            long agendaItemsCount = meetingProjectRepository.countByMeetingId(meetingId);
            List<MeetingAttendee> attendees = meetingAttendeeRepository.findByMeetingId(meetingId);
            List<MeetingAttendance> attendances = attendanceRepository.findByMeetingId(meetingId);

            // Calculs détaillés
            long manualInvitations = attendees.stream().mapToLong(a -> a.getAddedManually() ? 1 : 0).sum();
            long automaticInvitations = attendees.size() - manualInvitations;
            long presentCount = attendances.stream().mapToLong(a -> a.isPresent() ? 1 : 0).sum();
            long absentCount = attendances.size() - presentCount;
            long justifiedAbsences = attendances.stream()
                    .mapToLong(a -> !a.isPresent() && a.isJustified() ? 1 : 0).sum();

            // Construction de la réponse
            stats.put("meetingId", meetingId);
            stats.put("agendaItemsCount", agendaItemsCount);
            stats.put("totalInvited", attendees.size());
            stats.put("manualInvitations", manualInvitations);
            stats.put("automaticInvitations", automaticInvitations);
            stats.put("totalAttendances", attendances.size());
            stats.put("presentCount", presentCount);
            stats.put("absentCount", absentCount);
            stats.put("justifiedAbsences", justifiedAbsences);
            stats.put("unjustifiedAbsences", absentCount - justifiedAbsences);

            if (attendances.size() > 0) {
                stats.put("attendanceRate", Math.round((presentCount * 100.0) / attendances.size()));
            } else {
                stats.put("attendanceRate", 0);
            }

            return stats;
        } catch (Exception e) {
            log.error("Erreur lors du calcul des statistiques pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            throw new RuntimeException("Impossible de calculer les statistiques", e);
        }
    }

    /**
     * Supprime définitivement un évaluateur du système
     * ⚠️ ATTENTION : Opération destructive !
     */
    @Transactional
    public void removeEvaluator(Long evaluatorId) {
        try {
            log.warn("Suppression définitive de l'évaluateur {} - Opération destructive !", evaluatorId);

            // Supprimer toutes les présences
            attendanceRepository.deleteByEvaluatorId(evaluatorId);
            log.debug("Présences supprimées pour l'évaluateur {}", evaluatorId);

            // Supprimer l'utilisateur
            userRepository.deleteById(evaluatorId);
            log.warn("Évaluateur {} supprimé définitivement du système", evaluatorId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'évaluateur {}: {}", evaluatorId, e.getMessage(), e);
            throw new RuntimeException("Impossible de supprimer l'évaluateur", e);
        }
    }

    // ========================================
    // FONCTIONNALITÉS AVANCÉES
    // ========================================

    /**
     * Envoie des rappels automatiques pour une réunion
     * Utilisé par le système de tâches planifiées
     */
    @Transactional(readOnly = true)
    public void sendMeetingReminders(Long meetingId) {
        try {
            log.info("Envoi des rappels pour la réunion ID: {}", meetingId);

            Meeting meeting = getMeetingById(meetingId);
            if (meeting == null) {
                log.warn("Impossible d'envoyer les rappels : réunion {} non trouvée", meetingId);
                return;
            }

            List<MeetingAttendee> attendees = getMeetingAttendees(meetingId);

            String subject = "Rappel: Réunion du " + meeting.getDate().toString();
            String message = String.format(
                    "Rappel : Une réunion est prévue le %s à %s.\n\nStatut : %s\n\nCordialement,\nComité d'Éthique",
                    meeting.getDate(), meeting.getTime(), meeting.getStatus()
            );

            int sentCount = 0;
            for (MeetingAttendee attendee : attendees) {
                try {
                    emailService.sendReminderEmail(attendee.getUser().getEmail(), subject, message);
                    sentCount++;
                } catch (Exception e) {
                    log.error("Erreur lors de l'envoi du rappel à {}: {}",
                            attendee.getUser().getEmail(), e.getMessage());
                }
            }

            log.info("Rappels envoyés avec succès : {}/{} pour la réunion {}",
                    sentCount, attendees.size(), meetingId);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des rappels pour la réunion {}: {}",
                    meetingId, e.getMessage(), e);
            // Ne pas propager l'exception pour les tâches planifiées
        }
    }

    /**
     * Vérifie et met à jour automatiquement les statuts des réunions passées
     * Tâche planifiée quotidienne
     */
    @Transactional
    public int updatePastMeetingsStatus() {
        try {
            log.info("Mise à jour automatique des statuts des réunions passées");

            List<Meeting> allMeetings = meetingRepository.findAll();
            int updateCount = 0;

            for (Meeting meeting : allMeetings) {
                if (isPastMeeting(meeting) && !"Terminée".equals(meeting.getStatus())) {
                    meeting.setStatus("Terminée");
                    meetingRepository.save(meeting);
                    updateCount++;
                    log.debug("Statut mis à jour vers 'Terminée' pour la réunion ID: {} ({})",
                            meeting.getId(), meeting.getDate());
                }
            }

            if (updateCount > 0) {
                log.info("Statuts mis à jour pour {} réunions passées", updateCount);
            } else {
                log.debug("Aucune réunion passée à mettre à jour");
            }

            return updateCount;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour des statuts des réunions passées: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Recherche des réunions selon des critères multiples
     */
    @Transactional(readOnly = true)
    public List<Meeting> searchMeetings(String status, LocalDate fromDate, LocalDate toDate, String searchTerm) {
        try {
            log.debug("Recherche de réunions - Statut: {}, Du: {}, Au: {}, Terme: {}",
                    status, fromDate, toDate, searchTerm);

            // Pour l'instant, implémentation basique - peut être étendue avec Specifications
            List<Meeting> allMeetings = meetingRepository.findAll();

            return allMeetings.stream()
                    .filter(meeting -> {
                        // Filtre par statut
                        if (status != null && !status.isEmpty() && !status.equals(meeting.getStatus())) {
                            return false;
                        }

                        // Filtre par date de début
                        if (fromDate != null && meeting.getDate().isBefore(fromDate)) {
                            return false;
                        }

                        // Filtre par date de fin
                        if (toDate != null && meeting.getDate().isAfter(toDate)) {
                            return false;
                        }

                        // Recherche textuelle
                        if (searchTerm != null && !searchTerm.isEmpty()) {
                            String term = searchTerm.toLowerCase();
                            return (meeting.getMonth() != null && meeting.getMonth().toLowerCase().contains(term)) ||
                                    (meeting.getStatus() != null && meeting.getStatus().toLowerCase().contains(term));
                        }

                        return true;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de réunions: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible d'effectuer la recherche", e);
        }
    }

    /**
     * Génère un PDF du planning des réunions
     */
    public byte[] generateMeetingsPdf(int year) {
        try {
            log.info("🔄 Génération du PDF pour l'année: {}", year);

            // Récupération des réunions
            List<Meeting> meetings = meetingRepository.findByYear(year);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Titre
            Paragraph title = new Paragraph("Planning des Réunions " + year)
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            document.add(title);

            // Info
            Paragraph info = new Paragraph("Comité d'Éthique - " + meetings.size() + " réunions")
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(info);

            // Tableau
            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 2, 2}));
            table.setWidth(UnitValue.createPercentValue(100));

            // En-têtes
            String[] headers = {"N°", "Date", "Heure", "Mois", "Statut"};
            for (String header : headers) {
                Cell cell = new Cell()
                        .add(new Paragraph(header).setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER);
                table.addCell(cell);
            }

            // Données
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            int numeroReunion = 1;
            for (Meeting meeting : meetings) {
                // N°
                table.addCell(new Cell().add(new Paragraph(String.valueOf(numeroReunion)))
                        .setTextAlignment(TextAlignment.CENTER));

                // Date
                String dateStr = meeting.getDate() != null ?
                        meeting.getDate().format(dateFormatter) : "Date non définie";
                table.addCell(new Cell().add(new Paragraph(dateStr)));

                // Heure
                String timeStr = meeting.getTime() != null ?
                        meeting.getTime().format(timeFormatter) : "Non définie";
                table.addCell(new Cell().add(new Paragraph(timeStr))
                        .setTextAlignment(TextAlignment.CENTER));

                // Mois
                String month = meeting.getMonth() != null ? meeting.getMonth() : "N/A";
                table.addCell(new Cell().add(new Paragraph(month))
                        .setTextAlignment(TextAlignment.CENTER));

                // Statut
                String status = meeting.getStatus() != null ? meeting.getStatus() : "Planifiée";
                Cell statusCell = new Cell().add(new Paragraph(status))
                        .setTextAlignment(TextAlignment.CENTER);

                if ("Terminée".equals(status)) {
                    statusCell.setBackgroundColor(ColorConstants.GREEN);
                } else if ("Annulée".equals(status)) {
                    statusCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                } else {
                    // Planifiée - couleur par défaut (blanc)
                    statusCell.setBackgroundColor(ColorConstants.WHITE);
                }

                table.addCell(statusCell);
                numeroReunion++;
            }

            document.add(table);

            // Pied de page
            Paragraph footer = new Paragraph("\n\nDocument généré le " +
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(footer);

            document.close();

            byte[] result = baos.toByteArray();
            log.info("✅ PDF généré - {} réunions, {} bytes", meetings.size(), result.length);

            return result;

        } catch (Exception e) {
            log.error("❌ Erreur génération PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsForEvaluator(Long evaluatorId, Integer year) {
        try {
            log.debug("Récupération des réunions pour l'évaluateur {} (année: {})", evaluatorId, year);

            // Vérifier que l'utilisateur est bien un évaluateur
            User evaluator = userRepository.findById(evaluatorId)
                    .orElseThrow(() -> new IllegalArgumentException("Évaluateur non trouvé"));

            if (!evaluator.getRoles().contains("EVALUATEUR")) {
                throw new IllegalArgumentException("L'utilisateur n'est pas un évaluateur");
            }

            List<Meeting> meetings;
            if (year != null) {
                meetings = meetingRepository.findByYearAndAttendeeId(year, evaluatorId);
            } else {
                meetings = meetingRepository.findByAttendeeId(evaluatorId);
            }

            // Charger les relations nécessaires
            meetings.forEach(meeting -> {
                Hibernate.initialize(meeting.getAgendaItems());
                meeting.getAgendaItems().forEach(mp -> {
                    if (mp.getProject() != null) {
                        Hibernate.initialize(mp.getProject());
                    }
                });
            });

            return meetings;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur technique lors de la récupération des réunions pour l'évaluateur {}",
                    evaluatorId, e);
            throw new RuntimeException("Erreur lors de la récupération des réunions", e);
        }
    }

    public byte[] generateMeetingsPdfWithHeaderFooter(int year) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);

        // Ajouter le gestionnaire d'événements pour header/footer
        pdf.addEventHandler(PdfDocumentEvent.START_PAGE, new HeaderFooterHandler());

        Document document = new Document(pdf);
        // Ajustez les marges pour accommoder le header et footer
        document.setMargins(100, 36, 70, 36); // Haut:100px, Bas:70px

        try {
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Récupération des réunions
            List<Meeting> meetings = meetingRepository.findByYear(year);

            // Titre
            Paragraph title = new Paragraph("Planning des Réunions " + year)
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            document.add(title);

            // Info
            Paragraph info = new Paragraph("Comité d'Éthique - " + meetings.size() + " réunions")
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(info);

            // Tableau (même code que dans generateMeetingsPdf)
            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 2, 2}));
            table.setWidth(UnitValue.createPercentValue(100));

            // En-têtes
            String[] headers = {"N°", "Date", "Heure", "Mois", "Statut"};
            for (String header : headers) {
                Cell cell = new Cell()
                        .add(new Paragraph(header).setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER);
                table.addCell(cell);
            }

            // Données
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            int numeroReunion = 1;
            for (Meeting meeting : meetings) {
                // N°
                table.addCell(new Cell().add(new Paragraph(String.valueOf(numeroReunion)))
                        .setTextAlignment(TextAlignment.CENTER));

                // Date
                String dateStr = meeting.getDate() != null ?
                        meeting.getDate().format(dateFormatter) : "Date non définie";
                table.addCell(new Cell().add(new Paragraph(dateStr)));

                // Heure
                String timeStr = meeting.getTime() != null ?
                        meeting.getTime().format(timeFormatter) : "Non définie";
                table.addCell(new Cell().add(new Paragraph(timeStr))
                        .setTextAlignment(TextAlignment.CENTER));

                // Mois
                String month = meeting.getMonth() != null ? meeting.getMonth() : "N/A";
                table.addCell(new Cell().add(new Paragraph(month))
                        .setTextAlignment(TextAlignment.CENTER));

                // Statut
                String status = meeting.getStatus() != null ? meeting.getStatus() : "Planifiée";
                Cell statusCell = new Cell().add(new Paragraph(status))
                        .setTextAlignment(TextAlignment.CENTER);

                if ("Terminée".equals(status)) {
                    statusCell.setBackgroundColor(ColorConstants.GREEN);
                } else if ("Annulée".equals(status)) {
                    statusCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                } else {
                    statusCell.setBackgroundColor(ColorConstants.WHITE);
                }

                table.addCell(statusCell);
                numeroReunion++;
            }

            document.add(table);

            // Pied de page
            Paragraph footer = new Paragraph("\n\nDocument généré le " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(footer);

            document.close();

            byte[] result = baos.toByteArray();
            log.info("✅ PDF généré avec header/footer - {} réunions, {} bytes", meetings.size(), result.length);

            return result;

        } catch (Exception e) {
            log.error("❌ Erreur génération PDF avec header/footer: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    private class HeaderFooterHandler implements IEventHandler {
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfPage page = docEvent.getPage();
            Rectangle pageSize = page.getPageSize();
            float marginLeft = 36; // Marge gauche de 36px (1 inch)
            float marginRight = 36; // Marge droite de 36px

            try {
                // === HEADER ===
                Image headerImg = new Image(ImageDataFactory.create(getClass().getResource("/static/header.png")));

                // Ajuster la largeur pour tenir compte des marges
                float headerWidth = pageSize.getWidth() - marginLeft - marginRight;
                headerImg.setWidth(headerWidth);

                // Calculer la hauteur proportionnelle
                float headerHeight = headerImg.getImageHeight() * (headerWidth / headerImg.getImageWidth());

                // Position Y: haut de page moins la hauteur de l'image
                float headerY = pageSize.getTop() - headerHeight;

                // Dessiner le header
                new Canvas(new PdfCanvas(page), pageSize)
                        .add(headerImg.setFixedPosition(marginLeft, headerY, headerWidth))
                        .close();

                // === FOOTER ===
                Image footerImg = new Image(ImageDataFactory.create(getClass().getResource("/static/footer.png")));

                // Ajuster la largeur pour tenir compte des marges
                float footerWidth = pageSize.getWidth() - marginLeft - marginRight;
                footerImg.setWidth(footerWidth);

                // Calculer la hauteur proportionnelle
                float footerHeight = footerImg.getImageHeight() * (footerWidth / footerImg.getImageWidth());

                // Position Y: bas de page
                float footerY = pageSize.getBottom();

                // Dessiner le footer
                new Canvas(new PdfCanvas(page), pageSize)
                        .add(footerImg.setFixedPosition(marginLeft, footerY, footerWidth))
                        .close();

            } catch (Exception e) {
                log.error("Erreur lors du chargement des images header/footer", e);
            }
        }
    }
}