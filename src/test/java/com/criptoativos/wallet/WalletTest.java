package com.criptoativos.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.criptoativos.asset.Asset;
import com.criptoativos.asset.CryptoAsset;
import com.criptoativos.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WalletTest {

    private static Asset btc() {
        return CryptoAsset.create("BTC", "Bitcoin", null, new BigDecimal("100"), "bitcoin");
    }

    private static Asset eth() {
        return CryptoAsset.create("ETH", "Ethereum", null, new BigDecimal("50"), "ethereum");
    }

    private static Wallet emptyWallet() {
        return Wallet.forUser(null);
    }

    @Test
    void creditIncreasesTheBalanceAtScaleTwo() {
        Wallet wallet = emptyWallet();

        wallet.credit(new BigDecimal("100.005"));

        assertThat(wallet.getCashBalance()).hasToString("100.00");
    }

    /** The original Carteira returned silently on insufficient funds, so callers never knew. */
    @Test
    void debitRejectsAnAmountLargerThanTheBalanceAndLeavesItUntouched() {
        Wallet wallet = emptyWallet();
        wallet.credit(new BigDecimal("50.00"));

        assertThatThrownBy(() -> wallet.debit(new BigDecimal("50.01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient funds");

        assertThat(wallet.getCashBalance()).hasToString("50.00");
    }

    @Test
    void debitOfTheExactBalanceIsAllowed() {
        Wallet wallet = emptyWallet();
        wallet.credit(new BigDecimal("50.00"));

        wallet.debit(new BigDecimal("50.00"));

        assertThat(wallet.getCashBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void creditAndDebitRejectNonPositiveAmounts() {
        Wallet wallet = emptyWallet();

        assertThatThrownBy(() -> wallet.credit(BigDecimal.ZERO)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> wallet.credit(new BigDecimal("-1")))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> wallet.debit(BigDecimal.ZERO)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addHoldingComputesAWeightedAverageCost() {
        Wallet wallet = emptyWallet();
        Asset btc = btc();

        wallet.addHolding(btc, new BigDecimal("1"), new BigDecimal("100"));
        wallet.addHolding(btc, new BigDecimal("1"), new BigDecimal("200"));

        Holding holding = wallet.findHolding(btc).orElseThrow();
        assertThat(holding.getQuantity()).isEqualByComparingTo("2");
        assertThat(holding.getAverageCost()).isEqualByComparingTo("150");
    }

    @Test
    void weightedAverageRespectsUnevenQuantities() {
        Wallet wallet = emptyWallet();
        Asset btc = btc();

        wallet.addHolding(btc, new BigDecimal("3"), new BigDecimal("100"));
        wallet.addHolding(btc, new BigDecimal("1"), new BigDecimal("200"));

        // (3*100 + 1*200) / 4 = 125
        assertThat(wallet.findHolding(btc).orElseThrow().getAverageCost()).isEqualByComparingTo("125");
    }

    /** The original HashMap<CriptoAtivo, Double> had no equals/hashCode, so this produced two entries. */
    @Test
    void buyingTheSameAssetTwiceDoesNotDuplicateTheHolding() {
        Wallet wallet = emptyWallet();
        Asset btc = btc();

        wallet.addHolding(btc, BigDecimal.ONE, new BigDecimal("100"));
        wallet.addHolding(btc, BigDecimal.ONE, new BigDecimal("100"));

        assertThat(wallet.getHoldings()).hasSize(1);
    }

    @Test
    void differentAssetsGetSeparateHoldings() {
        Wallet wallet = emptyWallet();

        wallet.addHolding(btc(), BigDecimal.ONE, new BigDecimal("100"));
        wallet.addHolding(eth(), BigDecimal.ONE, new BigDecimal("50"));

        assertThat(wallet.getHoldings()).hasSize(2);
    }

    @Test
    void reduceHoldingRemovesTheHoldingWhenFullySold() {
        Wallet wallet = emptyWallet();
        Asset btc = btc();
        wallet.addHolding(btc, new BigDecimal("2"), new BigDecimal("100"));

        wallet.reduceHolding(btc, new BigDecimal("2"));

        assertThat(wallet.getHoldings()).isEmpty();
    }

    @Test
    void reduceHoldingKeepsThePositionOnAPartialSell() {
        Wallet wallet = emptyWallet();
        Asset btc = btc();
        wallet.addHolding(btc, new BigDecimal("2"), new BigDecimal("100"));

        wallet.reduceHolding(btc, new BigDecimal("0.5"));

        Holding holding = wallet.findHolding(btc).orElseThrow();
        assertThat(holding.getQuantity()).isEqualByComparingTo("1.5");
        assertThat(holding.getAverageCost()).as("selling does not change cost basis").isEqualByComparingTo("100");
    }

    @Test
    void reduceHoldingRejectsSellingMoreThanIsHeld() {
        Wallet wallet = emptyWallet();
        Asset btc = btc();
        wallet.addHolding(btc, new BigDecimal("1"), new BigDecimal("100"));

        assertThatThrownBy(() -> wallet.reduceHolding(btc, new BigDecimal("1.5")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient holdings");
    }

    @Test
    void reduceHoldingRejectsAnAssetTheWalletDoesNotOwn() {
        Wallet wallet = emptyWallet();

        assertThatThrownBy(() -> wallet.reduceHolding(btc(), BigDecimal.ONE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("you do not own BTC");
    }

    @Test
    void holdingsAreNotMutableFromOutside() {
        Wallet wallet = emptyWallet();
        wallet.addHolding(btc(), BigDecimal.ONE, new BigDecimal("100"));

        assertThatThrownBy(() -> wallet.getHoldings().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
