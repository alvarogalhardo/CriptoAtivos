package com.criptoativos.asset;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.criptoativos.support.AbstractIT;
import com.criptoativos.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class AssetIT extends AbstractIT {

    /** DOT is deliberately not in the V2 seed, so creating it does not collide. */
    private static final String NEW_ASSET =
            """
            {"symbol":"DOT","name":"Polkadot","description":"Multi-chain protocol.","currentPrice":"6.25","externalId":"polkadot"}
            """;

    @Autowired MockMvc mockMvc;
    @Autowired TestFixtures fixtures;

    @Test
    void listsSeededAssetsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].symbol").value(hasItem("BTC")))
                .andExpect(jsonPath("$.content[*].symbol").value(hasItem("ETH")));
    }

    @Test
    void looksUpBySymbolCaseInsensitively() throws Exception {
        mockMvc.perform(get("/api/v1/assets/btc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.name").value("Bitcoin"))
                .andExpect(jsonPath("$.category").value("CRYPTO"));
    }

    @Test
    void returns404ProblemDetailForAnUnknownSymbol() throws Exception {
        mockMvc.perform(get("/api/v1/assets/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Asset NOPE does not exist."));
    }

    @Test
    void rejectsAssetCreationWithoutAToken() throws Exception {
        mockMvc.perform(
                        post("/api/v1/assets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(NEW_ASSET))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAssetCreationForANonAdmin() throws Exception {
        mockMvc.perform(
                        post("/api/v1/assets")
                                .header(HttpHeaders.AUTHORIZATION, fixtures.userBearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(NEW_ASSET))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAnAdminToCreateAnAsset() throws Exception {
        mockMvc.perform(
                        post("/api/v1/assets")
                                .header(HttpHeaders.AUTHORIZATION, fixtures.adminBearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(NEW_ASSET))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("DOT"))
                .andExpect(jsonPath("$.category").value("CRYPTO"))
                .andExpect(jsonPath("$.currentPrice").value(6.25));
    }

    @Test
    void rejectsADuplicateSymbolWith409() throws Exception {
        mockMvc.perform(
                        post("/api/v1/assets")
                                .header(HttpHeaders.AUTHORIZATION, fixtures.adminBearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(NEW_ASSET.replace("\"DOT\"", "\"BTC\"")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Asset BTC already exists."));
    }

    @Test
    void anAdminCanSetAPriceDirectly() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/assets/BTC/price")
                                .header(HttpHeaders.AUTHORIZATION, fixtures.adminBearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"price\":\"64250.12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice").value(64250.12))
                .andExpect(jsonPath("$.priceUpdatedAt").isNotEmpty());
    }

    @Test
    void aNegativePriceIsRejected() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/assets/BTC/price")
                                .header(HttpHeaders.AUTHORIZATION, fixtures.adminBearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"price\":\"-1\"}"))
                .andExpect(status().isBadRequest());
    }
}
