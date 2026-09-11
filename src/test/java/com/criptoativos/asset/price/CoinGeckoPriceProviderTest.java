package com.criptoativos.asset.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CoinGeckoPriceProviderTest {

    private static final String BODY =
            """
            {"bitcoin":{"usd":64250.12,"usd_24h_change":2.4517},
             "ethereum":{"usd":3120.55,"usd_24h_change":-1.2033}}
            """;

    private MockRestServiceServer server;
    private CoinGeckoPriceProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder =
                RestClient.builder().baseUrl("https://api.coingecko.com/api/v3");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new CoinGeckoPriceProvider(builder.build(), "usd");
    }

    @Test
    void mapsTheResponseIntoQuotes() {
        server.expect(requestTo(Matchers.containsString("ids=bitcoin,ethereum")))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        Map<String, PriceProvider.PriceQuote> quotes =
                provider.fetchQuotes(List.of("bitcoin", "ethereum"));

        assertThat(quotes).hasSize(2);
        assertThat(quotes.get("bitcoin").price()).isEqualByComparingTo("64250.12");
        assertThat(quotes.get("bitcoin").dailyChangePct()).isEqualByComparingTo("2.4517");
        assertThat(quotes.get("ethereum").price()).isEqualByComparingTo("3120.55");
        assertThat(quotes.get("ethereum").dailyChangePct()).isEqualByComparingTo("-1.2033");
    }

    @Test
    void requestsTheConfiguredCurrencyAndDailyChange() {
        server.expect(
                        requestTo(
                                Matchers.allOf(
                                        Matchers.containsString("vs_currencies=usd"),
                                        Matchers.containsString("include_24hr_change=true"))))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        provider.fetchQuotes(List.of("bitcoin", "ethereum"));

        server.verify();
    }

    /** An outage must degrade to stale prices, never to a failing endpoint. */
    @Test
    void returnsAnEmptyMapWhenTheProviderIsDown() {
        server.expect(requestTo(Matchers.any(String.class))).andRespond(withServerError());

        assertThat(provider.fetchQuotes(List.of("bitcoin"))).isEmpty();
    }

    @Test
    void returnsAnEmptyMapForAMalformedResponse() {
        server.expect(requestTo(Matchers.any(String.class)))
                .andRespond(withSuccess("not json at all", MediaType.APPLICATION_JSON));

        assertThat(provider.fetchQuotes(List.of("bitcoin"))).isEmpty();
    }

    @Test
    void omitsCoinsThatCameBackWithoutAPrice() {
        server.expect(requestTo(Matchers.any(String.class)))
                .andRespond(
                        withSuccess(
                                "{\"bitcoin\":{\"usd\":100.0},\"dogecoin\":{}}",
                                MediaType.APPLICATION_JSON));

        Map<String, PriceProvider.PriceQuote> quotes =
                provider.fetchQuotes(List.of("bitcoin", "dogecoin"));

        assertThat(quotes).containsOnlyKeys("bitcoin");
    }

    @Test
    void skipsTheCallEntirelyForAnEmptyIdList() {
        server.expect(ExpectedCount.never(), requestTo(Matchers.any(String.class)));

        assertThat(provider.fetchQuotes(List.of())).isEmpty();
        assertThat(provider.fetchQuotes(null)).isEmpty();

        server.verify();
    }
}
