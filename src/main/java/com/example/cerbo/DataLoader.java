package com.example.cerbo;


import com.example.cerbo.entity.User;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.RoleType;
import com.example.cerbo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        createAdminIfNotExists("admin@example.com", "Admin123!");
    }

    private void createAdminIfNotExists(String email, String password) {
        if (userRepository.findByEmail(email) == null) {
            User admin = new User();
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setNom("Admin");
            admin.setPrenom("System");
            admin.setCivilite("Mr");
            admin.setValidated(true);

            Set<String> roles = new HashSet<>();
            roles.add(RoleType.ADMIN.name());
            admin.setRoles(roles);

            userRepository.save(admin);
            System.out.println("Admin account created successfully");
        } else {
            System.out.println("Admin account already exists");
        }
    }
}