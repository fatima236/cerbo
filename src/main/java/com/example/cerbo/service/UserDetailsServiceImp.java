package com.example.cerbo.service;

import com.example.cerbo.entity.PasswordResetToken;
import com.example.cerbo.entity.PendingUser;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.PasswordResetTokenRepository;
import com.example.cerbo.repository.PendingUserRepository;
import com.example.cerbo.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImp implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImp.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PendingUserRepository pendingUserRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null || !user.isValidated()) {
            throw new UsernameNotFoundException("User not found or not validated: " + email);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),          // Utilise l'email comme "username"
                user.getPassword(),       // Mot de passe crypté (bcrypt en général)
                mapRolesToAuthorities(user.getRoles()) // Convertir les rôles en authorities
        );
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Collection<String> roles) {
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public User createUser(String email, String password, Set<String> roles) {
        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("Cet utilisateur existe déjà !");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(roles);
        user.setValidated(true);

        return userRepository.save(user);
    }
    @Autowired
    private GmailVerificationService gmailVerificationService;
    public void requestInvestigateurSignup(User userRequest) {
        // 1. Vérification du format
        if (!userRequest.getEmail().endsWith("@gmail.com") &&
                !userRequest.getEmail().endsWith("@gmail.ma")) {
            throw new IllegalArgumentException("Seuls les emails Gmail sont acceptés");
        }

        // 2. Vérification de l'existence du compte

        if (pendingUserRepository.existsByEmail(userRequest.getEmail()) ||
                userRepository.existsByEmail(userRequest.getEmail())) {
            throw new IllegalArgumentException("Un compte ou une demande existe déjà pour cet email");
        }

        PendingUser pendingUser = new PendingUser();
        pendingUser.setEmail(userRequest.getEmail());
        pendingUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        pendingUser.setRequestDate(LocalDateTime.now());

        pendingUserRepository.save(pendingUser);
        sendValidationRequestToAdmin(pendingUser);
    }

    private void sendValidationRequestToAdmin(PendingUser pendingUser) {
        try {
            String emailContent = "<html>" +
                    "<body style='font-family: Arial, sans-serif;'>" +
                    "<h2 style='color: #333;'>Action requise: Approbation d'inscription</h2>" +
                    "<div style='background: #f5f5f5; padding: 20px; border-radius: 5px; margin-bottom: 20px;'>" +
                    "<p><strong>Email du candidat:</strong> " + pendingUser.getEmail() + "</p>" +
                    "<p><strong>Date de demande:</strong> " + pendingUser.getRequestDate() + "</p>" +
                    "</div>" +
                    "<p>Veuillez cliquer sur l'un des boutons ci-dessous:</p>" +
                    "<div style='margin-top: 20px;'>" +
                    "<a href='http://localhost:8081/api/auth/approve/" + pendingUser.getId() + "' " +
                    "style='background-color: #4CAF50; color: white; padding: 10px 20px; " +
                    "text-decoration: none; margin-right: 10px; border-radius: 5px; " +
                    "display: inline-block;'>Approuver</a>" +
                    "<a href='http://localhost:8081/api/auth/reject/" + pendingUser.getId() + "' " +
                    "style='background-color: #f44336; color: white; padding: 10px 20px; " +
                    "text-decoration: none; border-radius: 5px; display: inline-block;'>Rejeter</a>" +
                    "</div>" +
                    "<p style='margin-top: 30px; font-size: 0.9em; color: #666;'>" +
                    "Note: En approuvant, un email de confirmation sera automatiquement envoye au candidat." +
                    "</p>" +
                    "</body>" +
                    "</html>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("bouayadi.fatimazahra23@ump.ac.ma");
            helper.setTo("fatimazahrabouayadi93@gmail.com");
            helper.setSubject("Demande d'inscription a approuver (#" + pendingUser.getId() + ")");
            helper.setText(emailContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Echec d'envoi d'email a l'admin", e);
            pendingUserRepository.delete(pendingUser);
            throw new RuntimeException("Echec d'envoi de la demande de validation");
        }
    }

    public User approveInvestigateur(Long pendingUserId) {
        PendingUser pendingUser = pendingUserRepository.findById(pendingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        User user = new User();
        user.setEmail(pendingUser.getEmail());
        user.setPassword(pendingUser.getPassword());
        user.setRoles(Set.of("INVESTIGATEUR"));
        user.setValidated(true);

        User savedUser = userRepository.save(user);
        pendingUserRepository.delete(pendingUser);
        sendApprovalConfirmation(savedUser.getEmail());

        return savedUser;
    }

    private void sendApprovalConfirmation(String userEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("bouayadi.fatimazahra23@ump.ac.ma");
            helper.setTo(userEmail);
            helper.setSubject("Votre inscription a ete approuvee !");

            String htmlContent = "<html>" +
                    "<body style=\"font-family: Arial, sans-serif;\">" +
                    "<h2 style=\"color: #2e6c80;\">Felicitations !</h2>" +
                    "<p>Votre inscription en tant qu'investigateur a ete approuvee.</p>" +
                    "<p>Vous pouvez maintenant vous connecter a votre compte :</p>" +
                    "<a href=\"http://localhost:3000/investigateur/dashboard\" " +
                    "style=\"background-color: #4CAF50; color: white; " +
                    "padding: 10px 20px; text-decoration: none; " +
                    "border-radius: 5px; display: inline-block;\">" +
                    "Se connecter" +
                    "</a>" +
                    "</body>" +
                    "</html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            logger.info("Email de confirmation envoye a: {}", userEmail);
        } catch (Exception e) {
            logger.error("Echec d'envoi de l'email de confirmation", e);
            throw new RuntimeException("Echec d'envoi de la confirmation");
        }
    }


    public void rejectInvestigateur(Long pendingUserId) {
        if (!pendingUserRepository.existsById(pendingUserId)) {
            throw new IllegalArgumentException("Demande introuvable");
        }
        pendingUserRepository.deleteById(pendingUserId);
    }

    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null || !user.getRoles().contains("INVESTIGATEUR")) {
            throw new IllegalArgumentException("Aucun investigateur trouvé avec cet email");
        }

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusHours(24));

        passwordResetTokenRepository.save(token);
        sendPasswordResetEmail(user.getEmail(), token.getToken());
    }

    private void sendPasswordResetEmail(String email, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("bouayadi.fatimazahra23@ump.ac.ma");
            helper.setTo(email);
            helper.setSubject("Reinitialisation de votre mot de passe CERBO");

            String resetLink = "http://localhost:3000/reset-password?token=" + token;

            String htmlContent = "<html>" +
                    "<body style=\"font-family: Arial, sans-serif;\">" +
                    "<h2 style=\"color: #2e6c80;\">Reinitialisation de mot de passe</h2>" +
                    "<p>Vous avez demande a reinitialiser votre mot de passe.</p>" +
                    "<p>Cliquez sur le lien ci-dessous pour choisir un nouveau mot de passe :</p>" +
                    "<a href=\"" + resetLink + "\" " +
                    "style=\"background-color: #4CAF50; color: white; " +
                    "padding: 10px 20px; text-decoration: none; " +
                    "border-radius: 5px; display: inline-block;\">" +
                    "Reinitialiser mon mot de passe" +
                    "</a>" +
                    "<p style=\"color: #666; font-size: 0.9em;\">" +
                    "Ce lien expirera dans 24 heures." +
                    "</p>" +
                    "</body>" +
                    "</html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Echec d'envoi d'email de reinitialisation", e);
            throw new RuntimeException("Echec d'envoi de l'email de reinitialisation");
        }
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token invalide ou expiré");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }


}