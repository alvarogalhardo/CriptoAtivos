package com.criptoativos.auth.twofactor;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** RFC 6238 time-based one-time passwords, compatible with any standard authenticator app. */
@Service
public class TotpService {

    private static final int SECRET_BYTES = 20; // 160 bits, per RFC 4226
    private static final int DRIFT_STEPS = 1; // tolerate +/- one 30s step of clock skew
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TimeBasedOneTimePasswordGenerator generator =
            new TimeBasedOneTimePasswordGenerator();
    private final String issuer;

    public TotpService(@Value("${app.security.twofactor.issuer}") String issuer) {
        this.issuer = issuer;
    }

    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return new Base32().encodeToString(bytes).replace("=", "");
    }

    public boolean verify(String base32Secret, String code) {
        if (base32Secret == null || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        try {
            SecretKeySpec key = new SecretKeySpec(new Base32().decode(base32Secret), "HmacSHA1");
            Instant now = Instant.now();
            long stepSeconds = generator.getTimeStep().toSeconds();
            for (int step = -DRIFT_STEPS; step <= DRIFT_STEPS; step++) {
                Instant at = now.plusSeconds(step * stepSeconds);
                String expected = "%06d".formatted(generator.generateOneTimePassword(key, at));
                if (constantTimeEquals(expected, code)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            // A malformed secret or code is a failed verification, never a 500.
            return false;
        }
    }

    /** The {@code otpauth://} URI an authenticator app consumes, usually via a QR code. */
    public String buildProvisioningUri(String email, String base32Secret) {
        return "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30"
                .formatted(encode(issuer + ":" + email), base32Secret, encode(issuer));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** Comparison that does not leak how many leading digits were correct. */
    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
