package com.criptoativos.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.criptoativos.asset.AssetService;
import com.criptoativos.asset.inventory.AssetInventoryService;
import com.criptoativos.common.exception.BusinessRuleException;
import com.criptoativos.common.exception.InsufficientInventoryException;
import com.criptoativos.common.exception.NotFoundException;
import com.criptoativos.support.AbstractIT;
import com.criptoativos.support.TestFixtures;
import com.criptoativos.wallet.Wallet;
import com.criptoativos.wallet.WalletService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TransactionServiceIT extends AbstractIT {

    @Autowired TransactionService transactionService;
    @Autowired WalletService walletService;
    @Autowired AssetService assetService;
    @Autowired AssetInventoryService inventoryService;
    @Autowired TestFixtures fixtures;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = fixtures.registerUser().getId();
        walletService.deposit(userId, new BigDecimal("10000.00"));
        assetService.updatePrice("BTC", new BigDecimal("1000.00"));
    }

    private Wallet wallet() {
        return walletService.requireByUserId(userId);
    }

    @Test
    void buyDebitsCashAndCreatesAHolding() {
        transactionService.buy(userId, "BTC", new BigDecimal("2"));

        Wallet wallet = wallet();
        assertThat(wallet.getCashBalance()).isEqualByComparingTo("8000.00");
        assertThat(wallet.getHoldings()).hasSize(1);
        assertThat(wallet.getHoldings().iterator().next().getQuantity()).isEqualByComparingTo("2");
    }

    /** Original bug: Main.java recorded the asset and printed success even when the buy failed. */
    @Test
    void buyWithInsufficientFundsChangesNothingAtAll() {
        assertThatThrownBy(() -> transactionService.buy(userId, "BTC", new BigDecimal("11")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient funds");

        Wallet wallet = wallet();
        assertThat(wallet.getCashBalance()).isEqualByComparingTo("10000.00");
        assertThat(wallet.getHoldings()).isEmpty();
        assertThat(transactionService.history(userId, null, null, null, null, Pageable.unpaged()))
                .isEmpty();
    }

    /** Original bug: Carteira sold at the price captured when the asset object was built. */
    @Test
    void sellUsesTheCurrentPriceNotThePurchasePrice() {
        transactionService.buy(userId, "BTC", new BigDecimal("2")); // at 1000
        assetService.updatePrice("BTC", new BigDecimal("1500.00"));

        transactionService.sell(userId, "BTC", new BigDecimal("2")); // must settle at 1500

        assertThat(wallet().getCashBalance()).isEqualByComparingTo("11000.00");
    }

    @Test
    void sellingEverythingRemovesTheHolding() {
        transactionService.buy(userId, "BTC", new BigDecimal("1"));

        transactionService.sell(userId, "BTC", new BigDecimal("1"));

        assertThat(wallet().getHoldings()).isEmpty();
    }

    @Test
    void aPartialSellKeepsTheRemainder() {
        transactionService.buy(userId, "BTC", new BigDecimal("2"));

        transactionService.sell(userId, "BTC", new BigDecimal("0.5"));

        assertThat(wallet().getHoldings().iterator().next().getQuantity())
                .isEqualByComparingTo("1.5");
    }

    @Test
    void sellingMoreThanIsHeldIsRejected() {
        transactionService.buy(userId, "BTC", new BigDecimal("1"));

        assertThatThrownBy(() -> transactionService.sell(userId, "BTC", new BigDecimal("2")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient holdings");
    }

    @Test
    void sellingAnAssetNeverBoughtIsRejected() {
        assertThatThrownBy(() -> transactionService.sell(userId, "ETH", BigDecimal.ONE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("you do not own ETH");
    }

    @Test
    void buyingTheSameAssetTwiceMergesIntoOneHolding() {
        transactionService.buy(userId, "BTC", new BigDecimal("1"));
        transactionService.buy(userId, "BTC", new BigDecimal("1"));

        assertThat(wallet().getHoldings()).hasSize(1);
    }

    @Test
    void aNonPositiveQuantityIsRejected() {
        assertThatThrownBy(() -> transactionService.buy(userId, "BTC", BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> transactionService.buy(userId, "BTC", new BigDecimal("-1")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void anUnknownSymbolIsRejected() {
        assertThatThrownBy(() -> transactionService.buy(userId, "NOPE", BigDecimal.ONE))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void historyRecordsBothLegsWithTheExecutedPrice() {
        transactionService.buy(userId, "BTC", new BigDecimal("2"));
        assetService.updatePrice("BTC", new BigDecimal("1500.00"));
        transactionService.sell(userId, "BTC", new BigDecimal("1"));

        var history =
                transactionService.history(userId, null, null, null, null, Pageable.unpaged());

        assertThat(history).hasSize(2);
        assertThat(history)
                .extracting(Transaction::getTransactionType, Transaction::getUnitPrice)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(
                                TransactionType.BUY, new BigDecimal("1000.00000000")),
                        org.assertj.core.groups.Tuple.tuple(
                                TransactionType.SELL, new BigDecimal("1500.00000000")));
    }

    @Test
    void historyCanBeFilteredByTypeAndSymbol() {
        transactionService.buy(userId, "BTC", BigDecimal.ONE);
        transactionService.sell(userId, "BTC", BigDecimal.ONE);

        assertThat(
                        transactionService.history(
                                userId, TransactionType.BUY, null, null, null, Pageable.unpaged()))
                .hasSize(1);
        assertThat(transactionService.history(userId, null, "ETH", null, null, Pageable.unpaged()))
                .isEmpty();
    }

    @Test
    void buyingDrawsDownTheExchangeInventory() {
        BigDecimal before = inventoryService.available("BTC");

        transactionService.buy(userId, "BTC", new BigDecimal("3"));

        assertThat(inventoryService.available("BTC"))
                .isEqualByComparingTo(before.subtract(new BigDecimal("3")));
    }

    @Test
    void sellingReturnsQuantityToTheExchangeInventory() {
        BigDecimal before = inventoryService.available("BTC");
        transactionService.buy(userId, "BTC", new BigDecimal("3"));

        transactionService.sell(userId, "BTC", new BigDecimal("3"));

        assertThat(inventoryService.available("BTC")).isEqualByComparingTo(before);
    }

    @Test
    void buyingMoreThanTheExchangeHoldsIsRejected() {
        inventoryService.restock("BTC", new BigDecimal("2"));

        assertThatThrownBy(() -> transactionService.buy(userId, "BTC", new BigDecimal("5")))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Only");
    }

    // Rollback and concurrency need real committed transactions; see TransactionIntegrityIT.
}
