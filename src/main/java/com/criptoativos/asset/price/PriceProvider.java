package com.criptoativos.asset.price;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * Port for external market data.
 *
 * <p>The domain never speaks HTTP: swapping CoinGecko for another source, or for nothing at all,
 * changes one adapter and no business logic. It is also what lets the whole test suite run offline.
 */
public interface PriceProvider {

    record PriceQuote(String externalId, BigDecimal price, BigDecimal dailyChangePct) {}

    /**
     * @return quotes keyed by external id. Ids the provider does not know are simply absent, and a
     *     provider failure yields an empty map rather than an exception — a market-data outage must
     *     degrade to stale prices, never to failing requests.
     */
    Map<String, PriceQuote> fetchQuotes(Collection<String> externalIds);
}
