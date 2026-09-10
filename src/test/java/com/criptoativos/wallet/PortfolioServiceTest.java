package com.criptoativos.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import com.criptoativos.asset.Asset;
import com.criptoativos.asset.CryptoAsset;
import com.criptoativos.wallet.dto.WalletDtos.HoldingResponse;
import com.criptoativos.wallet.dto.WalletDtos.WalletResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PortfolioServiceTest {

    private final PortfolioService portfolioService = new PortfolioService();

    private static Asset assetAt(String symbol, String name, String price) {
        Asset asset = CryptoAsset.create(symbol, name, null, new BigDecimal(price), symbol.toLowerCase());
        return asset;
    }

    @Test
    void anEmptyWalletValuesToZeroWithoutDividingByZero() {
        WalletResponse response = portfolioService.summarise(Wallet.forUser(null));

        assertThat(response.cashBalance()).isEqualByComparingTo("0.00");
        assertThat(response.investedValue()).isEqualByComparingTo("0.00");
        assertThat(response.marketValue()).isEqualByComparingTo("0.00");
        assertThat(response.totalValue()).isEqualByComparingTo("0.00");
        assertThat(response.unrealisedPnl()).isEqualByComparingTo("0.00");
        assertThat(response.unrealisedPnlPct()).isEqualByComparingTo("0");
        assertThat(response.holdings()).isEmpty();
    }

    @Test
    void cashOnlyCountsTowardsTotalValue() {
        Wallet wallet = Wallet.forUser(null);
        wallet.credit(new BigDecimal("500.00"));

        WalletResponse response = portfolioService.summarise(wallet);

        assertThat(response.totalValue()).isEqualByComparingTo("500.00");
        assertThat(response.marketValue()).isEqualByComparingTo("0.00");
    }

    @Test
    void aGainIsReportedAsPositivePnl() {
        Wallet wallet = Wallet.forUser(null);
        Asset btc = assetAt("BTC", "Bitcoin", "150");
        wallet.addHolding(btc, new BigDecimal("2"), new BigDecimal("100"));

        WalletResponse response = portfolioService.summarise(wallet);

        assertThat(response.investedValue()).isEqualByComparingTo("200.00");
        assertThat(response.marketValue()).isEqualByComparingTo("300.00");
        assertThat(response.unrealisedPnl()).isEqualByComparingTo("100.00");
        assertThat(response.unrealisedPnlPct()).isEqualByComparingTo("50.0000");
    }

    @Test
    void aLossIsReportedAsNegativePnl() {
        Wallet wallet = Wallet.forUser(null);
        Asset btc = assetAt("BTC", "Bitcoin", "75");
        wallet.addHolding(btc, new BigDecimal("2"), new BigDecimal("100"));

        WalletResponse response = portfolioService.summarise(wallet);

        assertThat(response.unrealisedPnl()).isEqualByComparingTo("-50.00");
        assertThat(response.unrealisedPnlPct()).isEqualByComparingTo("-25.0000");
    }

    @Test
    void totalValueIsCashPlusMarketValue() {
        Wallet wallet = Wallet.forUser(null);
        wallet.credit(new BigDecimal("1000.00"));
        wallet.addHolding(assetAt("BTC", "Bitcoin", "150"), new BigDecimal("2"), new BigDecimal("100"));

        assertThat(portfolioService.summarise(wallet).totalValue()).isEqualByComparingTo("1300.00");
    }

    @Test
    void eachHoldingIsValuedIndependently() {
        Wallet wallet = Wallet.forUser(null);
        wallet.addHolding(assetAt("BTC", "Bitcoin", "150"), new BigDecimal("2"), new BigDecimal("100"));
        wallet.addHolding(assetAt("ETH", "Ethereum", "40"), new BigDecimal("10"), new BigDecimal("50"));

        WalletResponse response = portfolioService.summarise(wallet);

        assertThat(response.holdings()).extracting(HoldingResponse::symbol).containsExactly("BTC", "ETH");

        HoldingResponse btc = response.holdings().get(0);
        assertThat(btc.marketValue()).isEqualByComparingTo("300.00");
        assertThat(btc.unrealisedPnl()).isEqualByComparingTo("100.00");

        HoldingResponse eth = response.holdings().get(1);
        assertThat(eth.marketValue()).isEqualByComparingTo("400.00");
        assertThat(eth.unrealisedPnl()).isEqualByComparingTo("-100.00");

        // Gain and loss cancel out across the portfolio.
        assertThat(response.unrealisedPnl()).isEqualByComparingTo("0.00");
        assertThat(response.unrealisedPnlPct()).isEqualByComparingTo("0.0000");
    }

    @Test
    void holdingsAreSortedBySymbol() {
        Wallet wallet = Wallet.forUser(null);
        wallet.addHolding(assetAt("XRP", "XRP", "1"), BigDecimal.ONE, BigDecimal.ONE);
        wallet.addHolding(assetAt("ADA", "Cardano", "1"), BigDecimal.ONE, BigDecimal.ONE);
        wallet.addHolding(assetAt("BTC", "Bitcoin", "1"), BigDecimal.ONE, BigDecimal.ONE);

        assertThat(portfolioService.summarise(wallet).holdings())
                .extracting(HoldingResponse::symbol)
                .containsExactly("ADA", "BTC", "XRP");
    }

    @Test
    void fractionalQuantitiesAreValuedAtCashScale() {
        Wallet wallet = Wallet.forUser(null);
        wallet.addHolding(
                assetAt("BTC", "Bitcoin", "64250.12"), new BigDecimal("0.00123456"), new BigDecimal("60000"));

        HoldingResponse holding = portfolioService.summarise(wallet).holdings().get(0);

        // 0.00123456 * 64250.12 = 79.320628... -> 79.32
        assertThat(holding.marketValue()).isEqualByComparingTo("79.32");
        // 0.00123456 * 60000 = 74.0736 -> 74.07
        assertThat(holding.investedValue()).isEqualByComparingTo("74.07");
    }
}
