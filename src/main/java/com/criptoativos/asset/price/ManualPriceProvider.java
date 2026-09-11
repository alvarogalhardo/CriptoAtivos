package com.criptoativos.asset.price;

import java.util.Collection;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No external market data at all: prices are whatever an admin set via {@code PATCH
 * /api/v1/assets/{symbol}/price}.
 *
 * <p>Selected with {@code app.prices.provider=manual}, which is how the test profile keeps the
 * suite offline and deterministic.
 */
@Component
@ConditionalOnProperty(name = "app.prices.provider", havingValue = "manual")
public class ManualPriceProvider implements PriceProvider {

    @Override
    public Map<String, PriceQuote> fetchQuotes(Collection<String> externalIds) {
        return Map.of();
    }
}
