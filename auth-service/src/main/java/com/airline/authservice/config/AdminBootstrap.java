package com.airline.authservice.config;

import com.airline.authservice.entity.Role;
import com.airline.authservice.entity.User;
import com.airline.authservice.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default ADMIN user on first startup when no admin account exists. Credentials are
 * configurable via environment variables so they can be overridden in deployed environments.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default.email:admin@airline.com}")
    private String defaultEmail;

    @Value("${admin.default.password:Admin123!}")
    private String defaultPassword;

    @Value("${admin.default.first-name:System}")
    private String defaultFirstName;

    @Value("${admin.default.last-name:Admin}")
    private String defaultLastName;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (userRepository.existsByEmail(defaultEmail)) {
            log.info("Admin account '{}' already exists — skipping bootstrap.", defaultEmail);
            return;
        }

        User admin = new User();
        admin.setFirstName(defaultFirstName);
        admin.setLastName(defaultLastName);
        admin.setEmail(defaultEmail);
        admin.setPassword(passwordEncoder.encode(defaultPassword));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        admin.setEmailVerified(true);

        userRepository.save(admin);

        log.info(
                "Default ADMIN user created: {}. Change the password immediately in production.",
                defaultEmail);
    }
}
