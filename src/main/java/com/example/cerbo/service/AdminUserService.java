package com.example.cerbo.service;


import com.example.cerbo.entity.PendingUser;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.RoleType;
import com.example.cerbo.repository.PendingUserRepository;
import com.example.cerbo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
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
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Récupère un utilisateur par son ID
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Récupère toutes les demandes d'inscription en attente
     */
    public List<PendingUser> getAllPendingUsers() {
        return pendingUserRepository.findAll();
    }

    /**
     * Récupère une demande d'inscription en attente par son ID
     */
    public Optional<PendingUser> getPendingUserById(Long id) {
        return pendingUserRepository.findById(id);
    }

    /**
     * Approuve une demande d'inscription d'investigateur
     */
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