package com.criptoativos.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Result of a password check.
 *
 * <p>Either an access token, or — when the account has 2FA enabled — a challenge token that must be
 * exchanged at {@code /auth/2fa/verify}. Unused fields are omitted from the JSON entirely, so a
 * client cannot mistake a challenge for a successful login.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        boolean twoFactorRequired,
        String challengeToken,
        String accessToken,
        String tokenType,
        Long expiresIn) {

    public static LoginResponse accessGranted(String token, long expiresIn) {
        return new LoginResponse(false, null, token, "Bearer", expiresIn);
    }

    public static LoginResponse challenge(String challengeToken) {
        return new LoginResponse(true, challengeToken, null, null, null);
    }
}
