package com.criptoativos.asset.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class AssetInventoryIT extends AbstractIT {

    @Autowired MockMvc mockMvc;
    @Autowired TestFixtures fixtures;

    @Test
    void seededAssetsExposeAnInventory() throws Exception {
        mockMvc.perform(get("/api/v1/assets/BTC/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.availableQuantity").value(1000));
    }

    @Test
    void anAdminCanRestock() throws Exception {
        mockMvc.perform(
                        put("/api/v1/assets/BTC/inventory")
                                .header(HttpHeaders.AUTHORIZATION, fixtures.adminBearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"availableQuantity\":\"250.5\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(250.5));
    }

    @Test
    void aPlainUserCannotRestock() throws Exception {
        mockMvc.perform(
                        put("/api/v1/assets/BTC/inventory")
                                .header(HttpHeaders.AUTHORIZATION, fixtures.userBearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"availableQuantity\":\"250\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void restockingRequiresAToken() throws Exception {
        mockMvc.perform(
                        put("/api/v1/assets/BTC/inventory")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"availableQuantity\":\"250\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restockingRejectsANegativeQuantity() throws Exception {
        mockMvc.perform(
                        put("/api/v1/assets/BTC/inventory")
                                .header(HttpHeaders.AUTHORIZATION, fixtures.adminBearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"availableQuantity\":\"-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.availableQuantity").value("must not be negative"));
    }

    @Test
    void unknownSymbolReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/assets/NOPE/inventory")).andExpect(status().isNotFound());
    }

    @Test
    void aNewlyCreatedAssetStartsWithZeroInventory() throws Exception {
        mockMvc.perform(
                        post("/api/v1/assets")
                                .header(HttpHeaders.AUTHORIZATION, fixtures.adminBearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"symbol":"AVAX","name":"Avalanche","currentPrice":"30.00","externalId":"avalanche-2"}
                                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/assets/AVAX/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(0));
    }
}
