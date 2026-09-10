package com.criptoativos.asset.price;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Reads spot prices from CoinGecko's free public API. */
@Component
@ConditionalOnProperty(name = "app.prices.provider", havingValue = "coingecko", matchIfMissing = true)
public class CoinGeckoPriceProvider implements PriceProvider {

    private static final Logger log = LoggerFactory.getLogger(CoinGeckoPriceProvider.class);

    private static final ParameterizedTypeReference<Map<String, Map<String, BigDecimal>>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String vsCurrency;

    public CoinGeckoPriceProvider(
            RestClient coinGeckoRestClient,
            @Value("${app.prices.coingecko.vs-currency}") String vsCurrency) {
        this.restClient = coinGeckoRestClient;
        this.vsCurrency = vsCurrency;
    }

    @Override
    public Map<String, PriceQuote> fetchQuotes(Collection<String> externalIds) {
        if (externalIds == null || externalIds.isEmpty()) {
            return Map.of(); // never make a pointless network call
        }
        String ids = String.join(",", externalIds);
        try {
            Map<String, Map<String, BigDecimal>> body =
                    restClient
                            .get()
                            .uri(
                                    uriBuilder ->
                                            uriBuilder
                                                    .path("/simple/price")
                                                    .queryParam("ids", ids)
                                                    .queryParam("vs_currencies", vsCurrency)
                                                    .queryParam("include_24hr_change", "true")
                                                    .build())
                            .retrieve()
                            .body(RESPONSE_TYPE);

            if (body == null) {
                return Map.of();
            }
            Map<String, PriceQuote> quotes = new HashMap<>();
            String changeKey = vsCurrency + "_24h_change";
            body.forEach(
                    (id, fields) -> {
                        BigDecimal price = fields.get(vsCurrency);
                        if (price != null) {
                            quotes.put(id, new PriceQuote(id, price, fields.get(changeKey)));
                        }
                    });
            return quotes;
        } catch (RestClientException ex) {
            // Degrade to stale prices; the scheduled job will try again.
            log.warn("CoinGecko price lookup failed ({}); keeping existing prices", ex.getMessage());
            return Map.of();
        }
    }
}
