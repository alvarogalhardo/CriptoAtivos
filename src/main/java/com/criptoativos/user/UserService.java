package com.criptoativos.user;

import com.criptoativos.common.exception.ConflictException;
import com.criptoativos.common.exception.NotFoundException;
import com.criptoativos.user.dto.RegisterRequest;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        String cpf = request.cpf().replaceAll("\\D", "");
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered.");
        }
        if (userRepository.existsByCpf(cpf)) {
            throw new ConflictException("CPF is already registered.");
        }

        User user =
                User.create(
                        request.name().trim(),
                        email,
                        passwordEncoder.encode(request.password()),
                        cpf,
                        Role.USER);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User requireById(UUID id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("User %s does not exist.".formatted(id)));
    }

    @Transactional
    public User rename(UUID id, String name) {
        User user = requireById(id);
        user.rename(name.trim());
        return user;
    }

    @Transactional(readOnly = true)
    public User requireByEmail(String email) {
        return userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("User %s does not exist.".formatted(email)));
    }
}
