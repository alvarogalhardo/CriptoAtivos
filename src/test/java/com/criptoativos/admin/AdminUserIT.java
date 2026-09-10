package com.criptoativos.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.criptoativos.asset.AssetService;
import com.criptoativos.support.AbstractIT;
import com.criptoativos.support.TestFixtures;
import com.criptoativos.user.User;
import com.criptoativos.wallet.WalletService;
import java.math.BigDecimal;
import java.util.UUID;
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
 * Deliberately not transactional: deletion cascades across four tables and the guardrails depend on
 * committed state, so these run against real transactions and clean up explicitly.
 */
@AutoConfigureMockMvc
class AdminUserIT extends AbstractIT {

    @Autowired MockMvc mockMvc;
    @Autowired TestFixtures fixtures;
    @Autowired WalletService walletService;
    @Autowired AssetService assetService;
    @Autowired JdbcTemplate jdbcTemplate;

    private User admin;
    private String adminBearer;

    @BeforeEach
    void setUp() {
        admin = fixtures.registerAdmin();
        adminBearer = fixtures.bearerFor(admin);
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
    void listingUsersRequiresAdmin() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, fixtures.userBearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listingUsersRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void anAdminSeesAPagedListWithMaskedCpfAndNoSecrets() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").isNotEmpty())
                .andExpect(
                        jsonPath("$.content[0].cpfMasked")
                                .value(matchesPattern("\\*{3}\\.\\*{3}\\.\\d{3}-\\d{2}")))
                .andExpect(jsonPath("$.content[0].cpf").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].twoFactorSecret").doesNotExist());
    }

    @Test
    void searchFiltersByEmailFragment() throws Exception {
        User target = fixtures.registerUser();
        String fragment = target.getEmail().substring(0, 8);

        mockMvc.perform(
                        get("/api/v1/admin/users")
                                .param("search", fragment)
                                .header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value(containsString(fragment)));
    }

    @Test
    void searchWithNoTermReturnsEveryone() throws Exception {
        fixtures.registerUser();

        mockMvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void holdingCountsAreReported() throws Exception {
        User trader = fixtures.registerUser();
        walletService.deposit(trader.getId(), new BigDecimal("5000.00"));
        mockMvc.perform(
                post("/api/v1/transactions/buy")
                        .header(HttpHeaders.AUTHORIZATION, fixtures.bearerFor(trader))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"BTC\",\"quantity\":\"1\"}"));

        mockMvc.perform(
                        get("/api/v1/admin/users/" + trader.getId())
                                .header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdingCount").value(1))
                .andExpect(jsonPath("$.cashBalance").value(4000.00));
    }

    @Test
    void anAdminCannotDeleteThemselves() throws Exception {
        mockMvc.perform(
                        delete("/api/v1/admin/users/" + admin.getId())
                                .header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("You cannot delete your own account."));
    }

    @Test
    void deletingAUserWithFundsIsRefusedWithoutForce() throws Exception {
        User funded = fixtures.registerUser();
        walletService.deposit(funded.getId(), new BigDecimal("500.00"));

        mockMvc.perform(
                        delete("/api/v1/admin/users/" + funded.getId())
                                .header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(containsString("non-zero balance")));

        mockMvc.perform(
                        delete("/api/v1/admin/users/" + funded.getId())
                                .param("force", "true")
                                .header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletingAnEmptyAccountCascades() throws Exception {
        UUID target = fixtures.registerUser().getId();

        mockMvc.perform(
                        delete("/api/v1/admin/users/" + target)
                                .header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/admin/users/" + target).header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isNotFound());

        Integer wallets =
                jdbcTemplate.queryForObject(
                        "select count(*) from wallets where user_id = ?", Integer.class, target);
        assertThat(wallets).isZero();
    }

    @Test
    void theLastAdminCannotBeDemoted() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/admin/users/" + admin.getId() + "/role")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"USER\"}")
                                .header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("The last administrator cannot be demoted."));
    }

    @Test
    void anAdminCanPromoteAUserAndThenBeDemoted() throws Exception {
        User promoted = fixtures.registerUser();

        mockMvc.perform(
                        patch("/api/v1/admin/users/" + promoted.getId() + "/role")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"ADMIN\"}")
                                .header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // A second administrator now exists, so demoting the first is allowed.
        mockMvc.perform(
                        patch("/api/v1/admin/users/" + admin.getId() + "/role")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"USER\"}")
                                .header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void anUnknownUserIdReturns404() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/users/" + UUID.randomUUID())
                                .header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isNotFound());
    }

    @Test
    void anInvalidRoleIsRejected() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/admin/users/" + admin.getId() + "/role")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"SUPERUSER\"}")
                                .header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isBadRequest());
    }
}
