package com.criptoativos.auth;

import com.criptoativos.auth.dto.LoginRequest;
import com.criptoativos.auth.dto.LoginResponse;
import com.criptoativos.user.User;
import com.criptoativos.user.UserRepository;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    /**
     * A well-formed BCrypt hash of a value nobody can supply. Verifying against it when the email
     * is unknown keeps the response time comparable to a real password check, so an attacker cannot
     * enumerate accounts by timing.
     */
    private static final String DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final String GENERIC_FAILURE = "Invalid email or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> candidate = userRepository.findByEmailIgnoreCase(request.email());
        String hash = candidate.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean matches = passwordEncoder.matches(request.password(), hash);

        if (candidate.isEmpty() || !matches) {
            // Identical message either way: never reveal whether the account exists.
            throw new BadCredentialsException(GENERIC_FAILURE);
        }

        User user = candidate.get();
        return user.isTwoFactorEnabled()
                ? LoginResponse.challenge(tokenService.generateChallengeToken(user))
                : LoginResponse.accessGranted(
                        tokenService.generateToken(user), tokenService.ttlSeconds());
    }
}
