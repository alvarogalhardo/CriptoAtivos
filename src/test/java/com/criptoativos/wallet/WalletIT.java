package com.criptoativos.wallet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.criptoativos.support.AbstractIT;
import com.criptoativos.support.TestFixtures;
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
class WalletIT extends AbstractIT {

    @Autowired MockMvc mockMvc;
    @Autowired TestFixtures fixtures;

    private String bearer;

    @BeforeEach
    void setUp() {
        bearer = fixtures.userBearer();
    }

    private org.springframework.test.web.servlet.ResultActions cash(String path, String amount)
            throws Exception {
        return mockMvc.perform(
                post("/api/v1/wallet/" + path)
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"" + amount + "\"}"));
    }

    @Test
    void aFreshWalletIsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/wallet").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(0))
                .andExpect(jsonPath("$.totalValue").value(0))
                .andExpect(jsonPath("$.unrealisedPnlPct").value(0))
                .andExpect(jsonPath("$.holdings").isEmpty());
    }

    @Test
    void depositIncreasesTheBalance() throws Exception {
        cash("deposits", "1000.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(1000.00));

        mockMvc.perform(get("/api/v1/wallet").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.cashBalance").value(1000.00));
    }

    @Test
    void depositsAccumulate() throws Exception {
        cash("deposits", "100.50");
        cash("deposits", "99.50").andExpect(jsonPath("$.cashBalance").value(200.00));
    }

    @Test
    void withdrawalReducesTheBalance() throws Exception {
        cash("deposits", "1000.00");

        cash("withdrawals", "250.25")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(749.75));
    }

    @Test
    void withdrawingMoreThanTheBalanceIsRejectedAndChangesNothing() throws Exception {
        cash("deposits", "100.00");

        cash("withdrawals", "100.01")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Business rule violated"))
                .andExpect(
                        jsonPath("$.detail")
                                .value(org.hamcrest.Matchers.containsString("Insufficient funds")));

        mockMvc.perform(get("/api/v1/wallet").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.cashBalance").value(100.00));
    }

    @Test
    void aZeroDepositIsRejectedByValidation() throws Exception {
        cash("deposits", "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("must be greater than zero"));
    }

    @Test
    void aNegativeDepositIsRejectedByValidation() throws Exception {
        cash("deposits", "-50").andExpect(status().isBadRequest());
    }

    @Test
    void nonNumericAmountIsRejectedWithoutAStackTrace() throws Exception {
        cash("deposits", "abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void theWalletRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/v1/wallet")).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/v1/wallet/deposits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"amount\":\"10\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oneUsersDepositDoesNotAffectAnother() throws Exception {
        cash("deposits", "500.00");
        String otherBearer = fixtures.userBearer();

        mockMvc.perform(get("/api/v1/wallet").header(HttpHeaders.AUTHORIZATION, otherBearer))
                .andExpect(jsonPath("$.cashBalance").value(0));
    }
}
