package com.criptoativos.auth.twofactor;

import com.criptoativos.auth.TokenService;
import com.criptoativos.auth.dto.LoginResponse;
import com.criptoativos.auth.twofactor.dto.TwoFactorDtos.CodeRequest;
import com.criptoativos.auth.twofactor.dto.TwoFactorDtos.RecoveryCodesResponse;
import com.criptoativos.auth.twofactor.dto.TwoFactorDtos.SetupResponse;
import com.criptoativos.auth.twofactor.dto.TwoFactorDtos.VerifyRequest;
import com.criptoativos.common.SecurityUtils;
import com.criptoativos.user.User;
import com.criptoativos.user.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/2fa")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final TokenService tokenService;
    private final UserService userService;

    public TwoFactorController(
            TwoFactorService twoFactorService, TokenService tokenService, UserService userService) {
        this.twoFactorService = twoFactorService;
        this.tokenService = tokenService;
        this.userService = userService;
    }

    /** Step 1: generate a secret. 2FA is not active until {@link #confirm} succeeds. */
    @PostMapping("/setup")
    public SetupResponse setup() {
        return twoFactorService.beginSetup(SecurityUtils.currentUserId());
    }

    /** Step 2: prove the authenticator works, which activates 2FA and issues recovery codes. */
    @PostMapping("/confirm")
    public RecoveryCodesResponse confirm(@Valid @RequestBody CodeRequest request) {
        return twoFactorService.confirm(SecurityUtils.currentUserId(), request.code());
    }

    @PostMapping("/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@Valid @RequestBody VerifyRequest request) {
        twoFactorService.disable(SecurityUtils.currentUserId(), request.code());
    }

    /**
     * Exchanges a challenge token plus a valid code for a real access token. Reachable only with the
     * {@code 2FA_CHALLENGE} authority.
     */
    @PostMapping("/verify")
    public LoginResponse verify(@Valid @RequestBody VerifyRequest request) {
        UUID userId = SecurityUtils.currentUserId();
        if (!twoFactorService.verifyAnyFactor(userId, request.code())) {
            throw new BadCredentialsException("Invalid verification code.");
        }
        User user = userService.requireById(userId);
        return LoginResponse.accessGranted(tokenService.generateToken(user), tokenService.ttlSeconds());
    }
}
