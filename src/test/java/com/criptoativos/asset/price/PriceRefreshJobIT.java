package com.criptoativos.asset.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.criptoativos.asset.AssetService;
import com.criptoativos.asset.CryptoAsset;
import com.criptoativos.support.AbstractIT;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Import(PriceRefreshJobIT.StubPriceProviderConfig.class)
class PriceRefreshJobIT extends AbstractIT {

    /** A provider whose answers each test controls, so the job is exercised with no network. */
    static class StubPriceProvider implements PriceProvider {

        final Map<String, PriceQuote> quotes = new HashMap<>();

        @Override
        public Map<String, PriceQuote> fetchQuotes(Collection<String> externalIds) {
            return Map.copyOf(quotes);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StubPriceProviderConfig {
        @Bean
        @Primary
        StubPriceProvider stubPriceProvider() {
            return new StubPriceProvider();
        }
    }

    @Autowired PriceRefreshJob job;
    @Autowired StubPriceProvider stub;
    @Autowired AssetService assetService;

    @BeforeEach
    void resetStub() {
        stub.quotes.clear();
    }

    @Test
    void appliesQuotesToTrackedAssets() {
        stub.quotes.put(
                "bitcoin",
                new PriceProvider.PriceQuote("bitcoin", new BigDecimal("64250.12"), new BigDecimal("2.4517")));

        job.refresh();

        CryptoAsset btc = (CryptoAsset) assetService.requireBySymbol("BTC");
        assertThat(btc.getCurrentPrice()).isEqualByComparingTo("64250.12");
        assertThat(btc.getDailyChangePct()).isEqualByComparingTo("2.4517");
        assertThat(btc.getPriceUpdatedAt()).isNotNull();
    }

    /** A coin missing from the response keeps whatever price it had. */
    @Test
    void leavesUnquotedAssetsUntouched() {
        assetService.updatePrice("ETH", new BigDecimal("3000.00"));
        stub.quotes.put(
                "bitcoin", new PriceProvider.PriceQuote("bitcoin", new BigDecimal("64250.12"), null));

        job.refresh();

        assertThat(assetService.requireBySymbol("ETH").getCurrentPrice()).isEqualByComparingTo("3000.00");
    }

    @Test
    void anEmptyProviderResponseIsNotAnError() {
        assetService.updatePrice("BTC", new BigDecimal("1234.56"));

        assertThatCode(() -> job.refresh()).doesNotThrowAnyException();

        assertThat(assetService.requireBySymbol("BTC").getCurrentPrice()).isEqualByComparingTo("1234.56");
    }

    @Test
    void aNullDailyChangeIsAccepted() {
        stub.quotes.put("bitcoin", new PriceProvider.PriceQuote("bitcoin", new BigDecimal("50000"), null));

        job.refresh();

        CryptoAsset btc = (CryptoAsset) assetService.requireBySymbol("BTC");
        assertThat(btc.getCurrentPrice()).isEqualByComparingTo("50000");
        assertThat(btc.getDailyChangePct()).isNull();
    }
}
