package com.criptoativos.auth.twofactor;

import static org.assertj.core.api.Assertions.assertThat;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.Test;

class TotpServiceTest {

    private final TotpService totpService = new TotpService("CriptoAtivos");

    /** Generates the code an authenticator app would show at {@code at}. */
    static String codeAt(String base32Secret, Instant at) throws Exception {
        TimeBasedOneTimePasswordGenerator generator = new TimeBasedOneTimePasswordGenerator();
        SecretKeySpec key = new SecretKeySpec(new Base32().decode(base32Secret), "HmacSHA1");
        return "%06d".formatted(generator.generateOneTimePassword(key, at));
    }

    @Test
    void generatesADistinctBase32SecretEachTime() {
        String first = totpService.generateSecret();
        String second = totpService.generateSecret();

        assertThat(first).hasSize(32).matches("[A-Z2-7]+");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void acceptsTheCurrentCode() throws Exception {
        String secret = totpService.generateSecret();

        assertThat(totpService.verify(secret, codeAt(secret, Instant.now()))).isTrue();
    }

    @Test
    void acceptsACodeOneStepOldToToleratePhoneClockDrift() throws Exception {
        String secret = totpService.generateSecret();

        assertThat(totpService.verify(secret, codeAt(secret, Instant.now().minusSeconds(30))))
                .isTrue();
    }

    @Test
    void rejectsACodeThatIsTooOld() throws Exception {
        String secret = totpService.generateSecret();

        assertThat(totpService.verify(secret, codeAt(secret, Instant.now().minusSeconds(300))))
                .isFalse();
    }

    @Test
    void rejectsGarbageWithoutThrowing() {
        String secret = totpService.generateSecret();

        assertThat(totpService.verify(secret, "000000")).isFalse();
        assertThat(totpService.verify(secret, "not-a-code")).isFalse();
        assertThat(totpService.verify(secret, "12345")).isFalse();
        assertThat(totpService.verify(secret, "")).isFalse();
        assertThat(totpService.verify(secret, null)).isFalse();
        assertThat(totpService.verify(null, "123456")).isFalse();
        assertThat(totpService.verify("not-base32!!", "123456")).isFalse();
    }

    @Test
    void aCodeFromADifferentSecretIsRejected() throws Exception {
        String mine = totpService.generateSecret();
        String theirs = totpService.generateSecret();

        assertThat(totpService.verify(mine, codeAt(theirs, Instant.now()))).isFalse();
    }

    @Test
    void buildsAScannableProvisioningUri() {
        String uri = totpService.buildProvisioningUri("ana@example.com", "JBSWY3DPEHPK3PXP");

        assertThat(uri)
                .startsWith("otpauth://totp/CriptoAtivos%3Aana%40example.com")
                .contains("secret=JBSWY3DPEHPK3PXP")
                .contains("issuer=CriptoAtivos")
                .contains("algorithm=SHA1")
                .contains("digits=6")
                .contains("period=30");
    }
}
