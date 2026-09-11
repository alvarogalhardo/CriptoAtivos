package com.criptoativos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.criptoativos.support.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Treats the published API surface as a contract: if an endpoint disappears or the security scheme
 * breaks, the build fails rather than the documentation quietly going stale.
 */
@AutoConfigureMockMvc
class OpenApiIT extends AbstractIT {

    @Autowired MockMvc mockMvc;

    @Test
    void theSpecIsPubliclyReadable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("CriptoAtivos API"))
                .andExpect(jsonPath("$.info.version").value("v1"));
    }

    @Test
    void everyMajorEndpointIsDocumented() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/2fa/setup'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/2fa/verify'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/assets'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/assets/{symbol}/inventory'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/wallet'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/wallet/deposits'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/transactions/buy'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/transactions/sell'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/transactions'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users'].get").exists());
    }

    @Test
    void theBearerSecuritySchemeIsDeclaredSoSwaggerUiCanAuthorise() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(
                        jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(
                        jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat")
                                .value("JWT"));
    }

    @Test
    void swaggerUiIsReachableWithoutAToken() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
