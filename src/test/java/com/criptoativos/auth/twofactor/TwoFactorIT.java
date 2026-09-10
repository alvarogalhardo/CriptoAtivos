package com.criptoativos.auth.twofactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.criptoativos.support.AbstractIT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class TwoFactorIT extends AbstractIT {

    private static final String PASSWORD = "s3cret-passw0rd";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void registerAndLogin() throws Exception {
        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"Ana Maria","email":"ana2fa@example.com","password":"s3cret-passw0rd","cpf":"52998224725"}
                                """));
        accessToken = login().get("accessToken").asText();
    }

    private JsonNode login() throws Exception {
        String body =
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\"ana2fa@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return objectMapper.readTree(body);
    }

    private String beginSetup() throws Exception {
        String body =
                mockMvc.perform(post("/api/v1/auth/2fa/setup").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.provisioningUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/")))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return objectMapper.readTree(body).get("secret").asText();
    }

    private List<String> enable() throws Exception {
        String secret = beginSetup();
        String body =
                mockMvc.perform(
                                post("/api/v1/auth/2fa/confirm")
                                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"code\":\"" + TotpServiceTest.codeAt(secret, Instant.now()) + "\"}"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        List<String> codes = new ArrayList<>();
        objectMapper.readTree(body).get("recoveryCodes").forEach(node -> codes.add(node.asText()));
        return codes;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void setupDoesNotEnableTwoFactorOnItsOwn() throws Exception {
        beginSetup();

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(jsonPath("$.twoFactorEnabled").value(false));
        assertThat(login().has("accessToken")).isTrue();
    }

    @Test
    void confirmingWithAWrongCodeLeavesTwoFactorOff() throws Exception {
        beginSetup();

        mockMvc.perform(
                        post("/api/v1/auth/2fa/confirm")
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"000000\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Invalid verification code."));

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(jsonPath("$.twoFactorEnabled").value(false));
    }

    @Test
    void confirmingWithARealCodeEnablesTwoFactorAndReturnsRecoveryCodes() throws Exception {
        List<String> codes = enable();

        assertThat(codes).hasSize(10).allMatch(code -> code.matches("[A-Z2-9]{5}-[A-Z2-9]{5}"));
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(jsonPath("$.twoFactorEnabled").value(true));
    }

    @Test
    void loginNowReturnsAChallengeInsteadOfAnAccessToken() throws Exception {
        enable();

        JsonNode response = login();

        assertThat(response.get("twoFactorRequired").asBoolean()).isTrue();
        assertThat(response.has("challengeToken")).isTrue();
        assertThat(response.has("accessToken")).as("no access token before the second factor").isFalse();
    }

    /** The whole point of a separate authority: a challenge must not act as a session. */
    @Test
    void theChallengeTokenCannotReachProtectedEndpoints() throws Exception {
        enable();
        String challenge = login().get("challengeToken").asText();

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(challenge)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAccessTokenCannotBeUsedToCompleteTheChallenge() throws Exception {
        enable();

        mockMvc.perform(
                        post("/api/v1/auth/2fa/verify")
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"123456\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void verifyingWithAValidCodeReturnsAWorkingAccessToken() throws Exception {
        String secret = beginSetup();
        mockMvc.perform(
                post("/api/v1/auth/2fa/confirm")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + TotpServiceTest.codeAt(secret, Instant.now()) + "\"}"));

        String challenge = login().get("challengeToken").asText();

        String body =
                mockMvc.perform(
                                post("/api/v1/auth/2fa/verify")
                                        .header(HttpHeaders.AUTHORIZATION, bearer(challenge))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"code\":\"" + TotpServiceTest.codeAt(secret, Instant.now()) + "\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").isNotEmpty())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String fresh = objectMapper.readTree(body).get("accessToken").asText();
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(fresh)))
                .andExpect(status().isOk());
    }

    @Test
    void verifyingWithAWrongCodeIsRejected() throws Exception {
        enable();
        String challenge = login().get("challengeToken").asText();

        mockMvc.perform(
                        post("/api/v1/auth/2fa/verify")
                                .header(HttpHeaders.AUTHORIZATION, bearer(challenge))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aRecoveryCodeWorksOnceAndThenIsRejected() throws Exception {
        String recoveryCode = enable().get(0);
        String challenge = login().get("challengeToken").asText();

        mockMvc.perform(
                        post("/api/v1/auth/2fa/verify")
                                .header(HttpHeaders.AUTHORIZATION, bearer(challenge))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"" + recoveryCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        String secondChallenge = login().get("challengeToken").asText();
        mockMvc.perform(
                        post("/api/v1/auth/2fa/verify")
                                .header(HttpHeaders.AUTHORIZATION, bearer(secondChallenge))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"" + recoveryCode + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disablingTwoFactorRestoresPlainLogin() throws Exception {
        String secret = beginSetup();
        String code = TotpServiceTest.codeAt(secret, Instant.now());
        mockMvc.perform(
                post("/api/v1/auth/2fa/confirm")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"));

        mockMvc.perform(
                        post("/api/v1/auth/2fa/disable")
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"" + TotpServiceTest.codeAt(secret, Instant.now()) + "\"}"))
                .andExpect(status().isNoContent());

        assertThat(login().has("accessToken")).isTrue();
    }

    @Test
    void setupIsRejectedWhenTwoFactorIsAlreadyEnabled() throws Exception {
        enable();

        mockMvc.perform(post("/api/v1/auth/2fa/setup").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void twoFactorEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/2fa/setup")).andExpect(status().isUnauthorized());
    }
}
