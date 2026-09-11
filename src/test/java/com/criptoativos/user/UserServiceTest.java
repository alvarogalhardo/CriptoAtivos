package com.criptoativos.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.criptoativos.common.exception.ConflictException;
import com.criptoativos.user.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final RegisterRequest REQUEST =
            new RegisterRequest("Ana Maria", "ana@example.com", "s3cret-passw0rd", "52998224725");

    @Mock UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    private void allowRegistration() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.existsByCpf(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void registerHashesThePasswordAndNeverStoresItInPlaintext() {
        allowRegistration();

        User created = userService.register(REQUEST);

        assertThat(created.getPasswordHash()).isNotEqualTo("s3cret-passw0rd").startsWith("$2");
        assertThat(passwordEncoder.matches("s3cret-passw0rd", created.getPasswordHash())).isTrue();
        assertThat(created.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registerStripsCpfFormattingAndNormalisesEmail() {
        allowRegistration();

        User created =
                userService.register(
                        new RegisterRequest(
                                "  Ana Maria  ",
                                "Ana@Example.COM",
                                "s3cret-passw0rd",
                                "529.982.247-25"));

        assertThat(created.getCpf()).isEqualTo("52998224725");
        assertThat(created.getEmail()).isEqualTo("ana@example.com");
        assertThat(created.getName()).isEqualTo("Ana Maria");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(REQUEST))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email is already registered");
    }

    @Test
    void registerRejectsDuplicateCpf() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.existsByCpf("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(REQUEST))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CPF is already registered");
    }

    @Test
    void registerCreatesAWalletWithAZeroBalance() {
        allowRegistration();

        User created = userService.register(REQUEST);

        assertThat(created.getWallet()).isNotNull();
        assertThat(created.getWallet().getCashBalance()).isEqualByComparingTo("0.00");
        assertThat(created.getWallet().getUser()).isSameAs(created);
    }
}
