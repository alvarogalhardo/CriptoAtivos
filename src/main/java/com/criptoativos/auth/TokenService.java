package com.criptoativos.auth;

import com.criptoativos.config.JwtProperties;
import com.criptoativos.user.User;
import java.time.Instant;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    /** Authority carried by a half-authenticated 2FA challenge token. */
    public static final String CHALLENGE_SCOPE = "2FA_CHALLENGE";

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    /** Full access token. The {@code scope} claim becomes the caller's granted authority. */
    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(properties.issuer())
                        .issuedAt(now)
                        .expiresAt(now.plus(properties.ttl()))
                        .subject(user.getId().toString())
                        .claim("email", user.getEmail())
                        .claim("scope", user.getRole().name())
                        .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * Issued after a correct password when 2FA is enabled. Its only authority is {@link
     * #CHALLENGE_SCOPE}, so it satisfies no protected route — it can be exchanged for a real token
     * at {@code /auth/2fa/verify} and nothing else.
     */
    public String generateChallengeToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(properties.issuer())
                        .issuedAt(now)
                        .expiresAt(now.plus(properties.challengeTtl()))
                        .subject(user.getId().toString())
                        .claim("scope", CHALLENGE_SCOPE)
                        .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public long ttlSeconds() {
        return properties.ttl().toSeconds();
    }
}
