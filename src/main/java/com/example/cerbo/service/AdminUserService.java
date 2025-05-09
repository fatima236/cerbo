package com.example.cerbo.service;

import com.example.cerbo.annotation.Loggable;
import com.example.cerbo.entity.PendingUser;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.RoleType;
import com.example.cerbo.repository.PendingUserRepository;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PendingUserRepository pendingUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Récupère tous les utilisateurs
     */
    @Loggable(actionType = "READ", entityType = "USER")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Récupère un utilisateur par son ID
     */
    @Loggable(actionType = "READ", entityType = "USER")
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Récupère toutes les demandes d'inscription en attente
     */
    @Loggable(actionType = "READ", entityType = "PENDING_USER")
    public List<PendingUser> getAllPendingUsers() {
        return pendingUserRepository.findAll();
    }

    /**
     * Récupère une demande d'inscription en attente par son ID
     */
    @Loggable(actionType = "READ", entityType = "PENDING_USER")
    public Optional<PendingUser> getPendingUserById(Long id) {
        return pendingUserRepository.findById(id);
    }

    /**
     * Approuve une demande d'inscription d'investigateur
     */
    @Loggable(actionType = "CREATE", entityType = "USER")
    @Transactional
    public User approveInvestigator(Long pendingUserId) {
        PendingUser pendingUser = pendingUserRepository.findById(pendingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'inscription non trouvée avec l'ID: " + pendingUserId));

        // Créer l'utilisateur final
        User user = new User();
        user.setEmail(pendingUser.getEmail());
        user.setPassword(pendingUser.getPassword()); // Pas besoin de réencoder car déjà encodé

        Set<String> roles = new HashSet<>();
        roles.add(RoleType.INVESTIGATEUR.name());
        user.setRoles(roles);
        user.setValidated(true);

        User savedUser = userRepository.save(user);
        pendingUserRepository.delete(pendingUser);

        return savedUser;
    }

    /**
     * Approuve une demande d'inscription en tant qu'évaluateur
     */
    @Loggable(actionType = "CREATE", entityType = "USER")
    @Transactional
    public User approveAsEvaluator(Long pendingUserId) {
        PendingUser pendingUser = pendingUserRepository.findById(pendingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'inscription non trouvée avec l'ID: " + pendingUserId));

        // Créer l'utilisateur final
        User user = new User();
        user.setEmail(pendingUser.getEmail());
        user.setPassword(pendingUser.getPassword()); // Pas besoin de réencoder car déjà encodé

        Set<String> roles = new HashSet<>();
        roles.add(RoleType.EVALUATEUR.name());
        user.setRoles(roles);
        user.setValidated(true);

        User savedUser = userRepository.save(user);
        pendingUserRepository.delete(pendingUser);

        return savedUser;
    }

    /**
     * Rejette une demande d'inscription
     */
    @Loggable(actionType = "DELETE", entityType = "PENDING_USER")
    @Transactional
    public void rejectPendingUser(Long pendingUserId) {
        if (!pendingUserRepository.existsById(pendingUserId)) {
            throw new IllegalArgumentException("Demande d'inscription non trouvée avec l'ID: " + pendingUserId);
        }

        pendingUserRepository.deleteById(pendingUserId);
    }

    /**
     * Crée un nouvel administrateur
     */
    @Loggable(actionType = "CREATE", entityType = "USER")
    @Transactional
    public User createAdmin(String email, String password) {
        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        Set<String> roles = new HashSet<>();
        roles.add(RoleType.ADMIN.name());
        user.setRoles(roles);
        user.setValidated(true);

        return userRepository.save(user);
    }

    /**
     * Crée un nouvel évaluateur
     */
    @Loggable(actionType = "CREATE", entityType = "USER")
    @Transactional
    public User createEvaluateur(String email, String password) {
        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        Set<String> roles = new HashSet<>();
        roles.add(RoleType.EVALUATEUR.name());
        user.setRoles(roles);
        user.setValidated(true);

        return userRepository.save(user);
    }

    /**
     * Modifie le rôle d'un utilisateur existant
     */
    @Loggable(actionType = "UPDATE", entityType = "USER")
    @Transactional
    public User changeUserRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + userId));

        try {
            RoleType roleType = RoleType.valueOf(newRole);
            Set<String> roles = new HashSet<>();
            roles.add(roleType.name());
            user.setRoles(roles);

            return userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rôle invalide: " + newRole);
        }
    }

    /**
     * Active ou désactive un utilisateur
     */
    @Loggable(actionType = "UPDATE", entityType = "USER")
    @Transactional
    public User setUserActivation(Long userId, boolean activated) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + userId));

        user.setValidated(activated);
        return userRepository.save(user);
    }

    /**
     * Supprime un utilisateur
     */
    @Loggable(actionType = "DELETE", entityType = "USER")
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + userId);
        }

        userRepository.deleteById(userId);
    }
    /**
     * Modifie l'email d'un utilisateur existant
     */
    @Loggable(actionType = "UPDATE", entityType = "USER")
    @Transactional
    public User changeUserEmail(Long userId, String newEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + userId));

        // Vérifier si le nouvel email est déjà utilisé par un autre utilisateur
        User existingUser = userRepository.findByEmail(newEmail);
        if (existingUser != null && !existingUser.getId().equals(userId)) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        user.setEmail(newEmail);
        return userRepository.save(user);
    }
}