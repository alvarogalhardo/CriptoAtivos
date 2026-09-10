package com.criptoativos.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.criptoativos.asset.AssetService;
import com.criptoativos.asset.inventory.AssetInventoryService;
import com.criptoativos.common.exception.BusinessRuleException;
import com.criptoativos.common.exception.InsufficientInventoryException;
import com.criptoativos.support.AbstractIT;
import com.criptoativos.support.TestFixtures;
import com.criptoativos.wallet.Wallet;
import com.criptoativos.wallet.WalletService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Transactional integrity and concurrency.
 *
 * <p>Deliberately <strong>not</strong> {@code @Transactional}: these tests need real committed
 * transactions. Inside a test-managed transaction the debit is merely flushed, never committed, so
 * a rollback assertion would prove nothing — and the concurrency test needs work visible across
 * threads. Cleanup is therefore explicit.
 */
class TransactionIntegrityIT extends AbstractIT {

    @Autowired TransactionService transactionService;
    @Autowired WalletService walletService;
    @Autowired AssetService assetService;
    @Autowired AssetInventoryService inventoryService;
    @Autowired TestFixtures fixtures;
    @Autowired JdbcTemplate jdbcTemplate;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = fixtures.registerUser().getId();
        walletService.deposit(userId, new BigDecimal("10000.00"));
        assetService.updatePrice("BTC", new BigDecimal("1000.00"));
        inventoryService.restock("BTC", new BigDecimal("1000"));
    }

    @AfterEach
    void cleanUp() {
        // Committed data must be removed by hand. Order respects the foreign keys.
        jdbcTemplate.update("delete from operations");
        jdbcTemplate.update("delete from holdings");
        jdbcTemplate.update("delete from user_recovery_codes");
        jdbcTemplate.update("delete from wallets");
        jdbcTemplate.update("delete from users");
        jdbcTemplate.update("update asset_inventory set available_quantity = 1000");
    }

    private Wallet wallet() {
        return walletService.requireByUserId(userId);
    }

    /**
     * The wallet is debited before the inventory check throws, so only a genuine rollback can leave
     * the balance whole.
     */
    @Test
    void aFailedBuyRollsBackTheDebitCompletely() {
        inventoryService.restock("BTC", new BigDecimal("2"));

        assertThatThrownBy(() -> transactionService.buy(userId, "BTC", new BigDecimal("5")))
                .isInstanceOf(InsufficientInventoryException.class);

        assertThat(wallet().getCashBalance()).isEqualByComparingTo("10000.00");
        assertThat(wallet().getHoldings()).isEmpty();
        assertThat(inventoryService.available("BTC")).isEqualByComparingTo("2");
        assertThat(countTransactions()).isZero();
    }

    @Test
    void aSuccessfulBuyIsDurable() {
        transactionService.buy(userId, "BTC", new BigDecimal("2"));

        assertThat(wallet().getCashBalance()).isEqualByComparingTo("8000.00");
        assertThat(countTransactions()).isEqualTo(1);
    }

    /**
     * Eight threads race to buy when the balance affords exactly one purchase. The pessimistic
     * wallet lock must serialise them so the balance can never go negative.
     */
    @Test
    void concurrentBuysCanNeverOverdrawTheWallet() throws Exception {
        assetService.updatePrice("BTC", new BigDecimal("6000.00")); // 10000 affords exactly one

        int threads = 8;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(
                        executor.submit(
                                () -> {
                                    barrier.await();
                                    try {
                                        transactionService.buy(userId, "BTC", BigDecimal.ONE);
                                        succeeded.incrementAndGet();
                                    } catch (BusinessRuleException expected) {
                                        rejected.incrementAndGet(); // losing threads are supposed
                                        // to lose
                                    }
                                    return null;
                                }));
            }
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(succeeded.get()).as("exactly one buy should win").isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(threads - 1);
        assertThat(wallet().getCashBalance()).isEqualByComparingTo("4000.00");
        assertThat(wallet().getCashBalance().signum()).isNotNegative();
    }

    /** The same race against finite supply rather than finite cash. */
    @Test
    void concurrentBuysCanNeverOversellTheInventory() throws Exception {
        assetService.updatePrice("BTC", new BigDecimal("1.00")); // cash is not the constraint here
        inventoryService.restock("BTC", new BigDecimal("3"));

        int threads = 8;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger succeeded = new AtomicInteger();

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(
                        executor.submit(
                                () -> {
                                    barrier.await();
                                    try {
                                        transactionService.buy(userId, "BTC", BigDecimal.ONE);
                                        succeeded.incrementAndGet();
                                    } catch (InsufficientInventoryException expected) {
                                        // supply ran out for this thread
                                    }
                                    return null;
                                }));
            }
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(succeeded.get()).isEqualTo(3);
        assertThat(inventoryService.available("BTC")).isEqualByComparingTo("0");
    }

    private Integer countTransactions() {
        return jdbcTemplate.queryForObject(
                "select count(*) from operations where user_id = ? and operation_type = 'TRANSACTION'",
                Integer.class,
                userId);
    }
}
