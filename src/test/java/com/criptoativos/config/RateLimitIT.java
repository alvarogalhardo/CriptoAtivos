package com.criptoativos.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.criptoativos.support.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enables the limiter the shared test profile turns off, at a threshold low enough to reach in a
 * few requests.
 */
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
        properties = {
            "app.security.rate-limit.enabled=true",
            "app.security.rate-limit.auth-requests-per-minute=3"
        })
class RateLimitIT extends AbstractIT {

    private static final String LOGIN_BODY =
            "{\"email\":\"nobody@example.com\",\"password\":\"whatever-password\"}";

    @Autowired MockMvc mockMvc;

    private void attemptLogin() throws Exception {
        mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY));
    }

    @Test
    void authEndpointsStopAcceptingRequestsPastTheLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            attemptLogin();
        }

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(LOGIN_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Too many requests"))
                .andExpect(jsonPath("$.status").value(429));
    }

    /** The limit is scoped to authentication, not to the whole API. */
    @Test
    void otherEndpointsAreNotRateLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/assets")).andExpect(status().isOk());
        }
    }
}
