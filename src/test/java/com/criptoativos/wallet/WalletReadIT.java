package com.criptoativos.wallet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.criptoativos.asset.AssetService;
import com.criptoativos.support.AbstractIT;
import com.criptoativos.support.TestFixtures;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Wallet reads without a surrounding test transaction.
 *
 * <p>{@code open-in-view} is disabled, so the Hibernate session closes when the service returns. A
 * {@code @Transactional} test keeps it open and would hide a {@code LazyInitializationException}
 * that real callers would hit — these tests run the way production does.
 */
@AutoConfigureMockMvc
class WalletReadIT extends AbstractIT {

    @Autowired MockMvc mockMvc;
    @Autowired TestFixtures fixtures;
    @Autowired AssetService assetService;
    @Autowired JdbcTemplate jdbcTemplate;

    private String bearer;

    @BeforeEach
    void setUp() {
        bearer = fixtures.userBearer();
        assetService.updatePrice("BTC", new BigDecimal("1000.00"));
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from operations");
        jdbcTemplate.update("delete from holdings");
        jdbcTemplate.update("delete from user_recovery_codes");
        jdbcTemplate.update("delete from wallets");
        jdbcTemplate.update("delete from users");
        jdbcTemplate.update("update asset_inventory set available_quantity = 1000");
    }

    @Test
    void anEmptyWalletSerialisesOutsideATransaction() throws Exception {
        mockMvc.perform(get("/api/v1/wallet").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(0))
                .andExpect(jsonPath("$.holdings").isEmpty());
    }

    @Test
    void depositSerialisesOutsideATransaction() throws Exception {
        mockMvc.perform(
                        post("/api/v1/wallet/deposits")
                                .header(HttpHeaders.AUTHORIZATION, bearer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"amount\":\"5000.00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(5000.00));
    }

    /** The case that actually broke: holdings must be initialised before the session closes. */
    @Test
    void aWalletWithHoldingsSerialisesOutsideATransaction() throws Exception {
        mockMvc.perform(
                post("/api/v1/wallet/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"5000.00\"}"));
        mockMvc.perform(
                        post("/api/v1/transactions/buy")
                                .header(HttpHeaders.AUTHORIZATION, bearer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"symbol\":\"BTC\",\"quantity\":\"2\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/wallet").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(3000.00))
                .andExpect(jsonPath("$.holdings[0].symbol").value("BTC"))
                .andExpect(jsonPath("$.holdings[0].quantity").value(2))
                .andExpect(jsonPath("$.holdings[0].averageCost").value(1000));
    }

    @Test
    void transactionHistorySerialisesOutsideATransaction() throws Exception {
        mockMvc.perform(
                post("/api/v1/wallet/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"5000.00\"}"));
        mockMvc.perform(
                post("/api/v1/transactions/buy")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"BTC\",\"quantity\":\"1\"}"));

        mockMvc.perform(get("/api/v1/transactions").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].symbol").value("BTC"))
                .andExpect(jsonPath("$.content[0].assetName").value("Bitcoin"))
                .andExpect(jsonPath("$.content[0].type").value("BUY"));
    }

    @Test
    void historyFilteringWorksWithAndWithoutFilters() throws Exception {
        mockMvc.perform(
                post("/api/v1/wallet/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"5000.00\"}"));
        mockMvc.perform(
                post("/api/v1/transactions/buy")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"BTC\",\"quantity\":\"1\"}"));

        mockMvc.perform(
                        get("/api/v1/transactions")
                                .param("type", "BUY")
                                .param("symbol", "btc")
                                .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(
                        get("/api/v1/transactions")
                                .param("symbol", "ETH")
                                .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
