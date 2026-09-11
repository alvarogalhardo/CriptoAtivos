package com.criptoativos.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.criptoativos.support.AbstractIT;
import com.criptoativos.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * A bad {@code sort} value is a client mistake and must read as one.
 *
 * <p>Swagger UI pre-fills the sort field with the literal placeholder {@code ["string"]}, so this
 * is the first request many people make against the API by accident.
 */
@AutoConfigureMockMvc
@Transactional
class SortValidationIT extends AbstractIT {

    @Autowired MockMvc mockMvc;
    @Autowired TestFixtures fixtures;

    @Test
    void theSwaggerPlaceholderSortValueReturns400NotServerError() throws Exception {
        mockMvc.perform(get("/api/v1/assets").param("sort", "[\"string\"]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid sort property"))
                .andExpect(jsonPath("$.property").value("[\"string\"]"))
                .andExpect(
                        jsonPath("$.detail")
                                .value(org.hamcrest.Matchers.containsString("Cannot sort by")));
    }

    @Test
    void aTypoInASortPropertyReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/assets").param("sort", "symbl,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.property").value("symbl"));
    }

    @Test
    void aValidSortStillWorks() throws Exception {
        mockMvc.perform(get("/api/v1/assets").param("sort", "symbol,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].symbol").value("XRP"));
    }

    @Test
    void badSortOnTransactionHistoryAlsoReturns400() throws Exception {
        mockMvc.perform(
                        get("/api/v1/transactions")
                                .param("sort", "nope,asc")
                                .header(HttpHeaders.AUTHORIZATION, fixtures.userBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid sort property"));
    }

    @Test
    void badSortOnTheAdminListingAlsoReturns400() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/users")
                                .param("sort", "nope,asc")
                                .header(HttpHeaders.AUTHORIZATION, fixtures.adminBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid sort property"));
    }
}
