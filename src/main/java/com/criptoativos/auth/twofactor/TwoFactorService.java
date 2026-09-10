package com.criptoativos.auth.twofactor;

import com.criptoativos.auth.twofactor.dto.TwoFactorDtos.RecoveryCodesResponse;
import com.criptoativos.auth.twofactor.dto.TwoFactorDtos.SetupResponse;
import com.criptoativos.common.exception.BusinessRuleException;
import com.criptoativos.common.exception.ConflictException;
import com.criptoativos.common.exception.NotFoundException;
import com.criptoativos.user.User;
import com.criptoativos.user.UserRepository;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TwoFactorService {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorService.class);

    private static final int CODE_GROUPS = 2;
    private static final int GROUP_LENGTH = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Excludes I, O, 0 and 1 so codes can be transcribed without ambiguity. */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UserRepository userRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final TotpService totpService;
    private final TextEncryptor encryptor;
    private final PasswordEncoder passwordEncoder;
    private final int recoveryCodeCount;

    public TwoFactorService(
            UserRepository userRepository,
            RecoveryCodeRepository recoveryCodeRepository,
            TotpService totpService,
            TextEncryptor encryptor,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.twofactor.recovery-code-count}") int recoveryCodeCount) {
        this.userRepository = userRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.totpService = totpService;
        this.encryptor = encryptor;
        this.passwordEncoder = passwordEncoder;
        this.recoveryCodeCount = recoveryCodeCount;
    }

    @Transactional
    public SetupResponse beginSetup(UUID userId) {
        User user = requireUser(userId);
        if (user.isTwoFactorEnabled()) {
            throw new ConflictException("Two-factor authentication is already enabled.");
        }
        String secret = totpService.generateSecret();
        user.stageTwoFactorSecret(encryptor.encrypt(secret));
        return new SetupResponse(secret, totpService.buildProvisioningUri(user.getEmail(), secret));
    }

    @Transactional
    public RecoveryCodesResponse confirm(UUID userId, String code) {
        User user = requireUser(userId);
        if (user.getTwoFactorSecret() == null) {
            throw new BusinessRuleException("Start two-factor setup before confirming.");
        }
        if (!totpService.verify(decryptSecret(user), code)) {
            throw new BusinessRuleException("Invalid verification code.");
        }
        user.confirmTwoFactor();
        log.info("2FA enabled for user={}", userId);
        return new RecoveryCodesResponse(regenerateRecoveryCodes(user));
    }

    @Transactional
    public void disable(UUID userId, String code) {
        User user = requireUser(userId);
        if (!user.isTwoFactorEnabled() || !verifyAnyFactor(user, code)) {
            throw new BusinessRuleException("Invalid verification code.");
        }
        user.disableTwoFactor();
        recoveryCodeRepository.deleteByUserId(userId);
        log.info("2FA disabled for user={}", userId);
    }

    /** Accepts a live TOTP code or an unused recovery code, consuming the latter. */
    @Transactional
    public boolean verifyAnyFactor(User user, String code) {
        if (user.getTwoFactorSecret() != null && totpService.verify(decryptSecret(user), code)) {
            return true;
        }
        for (RecoveryCode candidate : recoveryCodeRepository.findByUserIdAndUsedAtIsNull(user.getId())) {
            if (passwordEncoder.matches(code, candidate.getCodeHash())) {
                candidate.markUsed();
                log.info("Recovery code consumed for user={}", user.getId());
                return true;
            }
        }
        return false;
    }

    @Transactional
    public boolean verifyAnyFactor(UUID userId, String code) {
        return verifyAnyFactor(requireUser(userId), code);
    }

    private List<String> regenerateRecoveryCodes(User user) {
        recoveryCodeRepository.deleteByUserId(user.getId());
        List<String> plaintext = new ArrayList<>();
        for (int i = 0; i < recoveryCodeCount; i++) {
            String code = randomCode();
            plaintext.add(code);
            recoveryCodeRepository.save(RecoveryCode.issue(user, passwordEncoder.encode(code)));
        }
        return plaintext; // shown once; only hashes are kept
    }

    private String decryptSecret(User user) {
        return encryptor.decrypt(user.getTwoFactorSecret());
    }

    private User requireUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("User %s does not exist.".formatted(userId)));
    }

    private static String randomCode() {
        StringBuilder builder = new StringBuilder();
        for (int group = 0; group < CODE_GROUPS; group++) {
            if (group > 0) {
                builder.append('-');
            }
            for (int i = 0; i < GROUP_LENGTH; i++) {
                builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
        }
        return builder.toString();
    }
}
