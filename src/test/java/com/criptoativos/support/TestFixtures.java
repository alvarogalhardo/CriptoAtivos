package com.criptoativos.support;

import com.criptoativos.auth.TokenService;
import com.criptoativos.user.Role;
import com.criptoativos.user.User;
import com.criptoativos.user.UserRepository;
import com.criptoativos.user.UserService;
import com.criptoativos.user.dto.RegisterRequest;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds the users and tokens integration tests need.
 *
 * <p>Exposed as a bean (see {@link ContainersConfig}) rather than statics so it participates in
 * each test's transaction and rolls back with it.
 */
public class TestFixtures {

    private final AtomicInteger sequence = new AtomicInteger();
    private final UserService userService;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public TestFixtures(
            UserService userService, UserRepository userRepository, TokenService tokenService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    public User registerUser() {
        int index = sequence.getAndIncrement();
        return userService.register(
                new RegisterRequest(
                        "Test User " + index,
                        "user%d-%s@example.com"
                                .formatted(index, UUID.randomUUID().toString().substring(0, 8)),
                        "s3cret-passw0rd",
                        generateCpf(index)));
    }

    /**
     * Builds a CPF with correct check digits, so fixtures never fail validation. The base is spread
     * by a prime step to avoid the repeated-digit sequences the validator rejects.
     */
    public static String generateCpf(int index) {
        String base = "%09d".formatted(100_000_000L + (long) index * 7_919L % 800_000_000L);
        String withFirst = base + checkDigit(base, 9);
        return withFirst + checkDigit(withFirst, 10);
    }

    private static char checkDigit(String digits, int position) {
        int sum = 0;
        int weight = position + 1;
        for (int i = 0; i < position; i++) {
            sum += (digits.charAt(i) - '0') * weight--;
        }
        int remainder = sum % 11;
        return (char) ('0' + (remainder < 2 ? 0 : 11 - remainder));
    }

    public User registerAdmin() {
        User admin = registerUser();
        admin.assignRole(Role.ADMIN);
        return userRepository.save(admin);
    }

    public String tokenFor(User user) {
        return tokenService.generateToken(user);
    }

    public String bearerFor(User user) {
        return "Bearer " + tokenFor(user);
    }

    public String userBearer() {
        return bearerFor(registerUser());
    }

    public String adminBearer() {
        return bearerFor(registerAdmin());
    }
}
