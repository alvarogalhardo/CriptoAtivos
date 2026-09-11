package com.criptoativos.auth.twofactor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/** Request and response payloads for the two-factor endpoints. */
public final class TwoFactorDtos {

    private TwoFactorDtos() {}

    /** Returned once, at enrolment. The secret is shown so it can be typed in manually. */
    public record SetupResponse(String secret, String provisioningUri) {}

    /** A six-digit TOTP code. */
    public record CodeRequest(@NotBlank @Pattern(regexp = "\\d{6}") String code) {}

    /** A TOTP code or a recovery code — the verify step accepts either. */
    public record VerifyRequest(@NotBlank String code) {}

    /** Shown exactly once; the plaintext is never recoverable afterwards. */
    public record RecoveryCodesResponse(List<String> recoveryCodes) {}
}
