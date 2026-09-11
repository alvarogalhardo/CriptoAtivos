package com.criptoativos.admin;

import com.criptoativos.user.Role;
import com.criptoativos.user.User;
import com.criptoativos.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator so a fresh database is immediately usable.
 *
 * <p>This is an application runner rather than a Flyway migration because the password must be
 * hashed with the real {@link PasswordEncoder}; SQL cannot compute a BCrypt hash.
 *
 * <p>Set {@code ADMIN_SEED_ENABLED=false} to disable it, and always set {@code ADMIN_PASSWORD} for
 * anything beyond a local demo.
 */
@Component
@ConditionalOnProperty(name = "app.admin.seed-enabled", havingValue = "true", matchIfMissing = true)
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);
    private static final String DEFAULT_PASSWORD = "change-me-immediately";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String cpf;

    public AdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String email,
            @Value("${app.admin.password}") String password,
            @Value("${app.admin.cpf}") String cpf) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.cpf = cpf;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        userRepository.save(
                User.create(
                        "Administrator",
                        email.toLowerCase(),
                        passwordEncoder.encode(password),
                        cpf,
                        Role.ADMIN));

        if (DEFAULT_PASSWORD.equals(password)) {
            log.warn(
                    "Seeded administrator {} with the DEFAULT password. Set ADMIN_PASSWORD, or "
                            + "ADMIN_SEED_ENABLED=false, before exposing this instance.",
                    email);
        } else {
            log.info("Seeded administrator account {}", email);
        }
    }
}
