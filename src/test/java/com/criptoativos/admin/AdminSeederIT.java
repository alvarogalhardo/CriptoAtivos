package com.criptoativos.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.criptoativos.support.AbstractIT;
import com.criptoativos.user.Role;
import com.criptoativos.user.User;
import com.criptoativos.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Re-enables seeding, which the shared test profile turns off. */
@Transactional
@TestPropertySource(
        properties = {
            "app.admin.seed-enabled=true",
            "app.admin.email=seeded-admin@criptoativos.test",
            "app.admin.password=a-real-seeded-password",
            "app.admin.cpf=16899535009"
        })
class AdminSeederIT extends AbstractIT {

    private static final String SEEDED_EMAIL = "seeded-admin@criptoativos.test";

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User seededAdmin() {
        return userRepository.findByEmailIgnoreCase(SEEDED_EMAIL).orElseThrow();
    }

    @Test
    void seedsAnAdministratorWithAHashedPassword() {
        User admin = seededAdmin();

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getPasswordHash()).startsWith("$2").isNotEqualTo("a-real-seeded-password");
        assertThat(passwordEncoder.matches("a-real-seeded-password", admin.getPasswordHash())).isTrue();
    }

    @Test
    void theSeededAdminGetsAWalletLikeAnyOtherAccount() {
        User admin = seededAdmin();

        assertThat(admin.getWallet()).isNotNull();
        assertThat(admin.getWallet().getCashBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void seedingRunsOnceAndDoesNotDuplicate() {
        assertThat(userRepository.countByRole(Role.ADMIN)).isEqualTo(1);
    }
}
