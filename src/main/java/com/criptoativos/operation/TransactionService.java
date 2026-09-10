package com.criptoativos.operation;

import com.criptoativos.asset.Asset;
import com.criptoativos.asset.AssetService;
import com.criptoativos.asset.inventory.AssetInventoryService;
import com.criptoativos.common.Money;
import com.criptoativos.common.exception.BusinessRuleException;
import com.criptoativos.common.exception.NotFoundException;
import com.criptoativos.wallet.Wallet;
import com.criptoativos.wallet.WalletRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Buying and selling.
 *
 * <p>Everything here runs in one transaction: if any step throws, nothing is recorded. The original
 * console app added the asset to its list <em>before</em> checking the balance and printed success
 * unconditionally, so a failed purchase still looked like it worked.
 *
 * <p><strong>Lock order is always wallet, then inventory.</strong> This is the only class that holds
 * both; reversing the order anywhere would let two concurrent trades on opposite assets deadlock.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final WalletRepository walletRepository;
    private final AssetService assetService;
    private final AssetInventoryService inventoryService;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            WalletRepository walletRepository,
            AssetService assetService,
            AssetInventoryService inventoryService,
            TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.assetService = assetService;
        this.inventoryService = inventoryService;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction buy(UUID userId, String symbol, BigDecimal quantity) {
        requirePositive(quantity);
        Asset asset = assetService.requireBySymbol(symbol);
        Wallet wallet = requireLockedWallet(userId); // lock 1

        BigDecimal unitPrice = asset.getCurrentPrice(); // price snapshot at execution time
        BigDecimal total = Money.cash(unitPrice.multiply(quantity));

        wallet.debit(total); // throws before anything is recorded
        inventoryService.reserve(asset, quantity); // lock 2; throws if supply is short
        wallet.addHolding(asset, quantity, unitPrice);

        Transaction saved =
                transactionRepository.save(
                        Transaction.of(
                                wallet.getUser(), asset, TransactionType.BUY, quantity, unitPrice, total));
        log.info(
                "BUY user={} asset={} qty={} unitPrice={} total={}",
                userId,
                asset.getSymbol(),
                quantity,
                unitPrice,
                total);
        return saved;
    }

    @Transactional
    public Transaction sell(UUID userId, String symbol, BigDecimal quantity) {
        requirePositive(quantity);
        Asset asset = assetService.requireBySymbol(symbol);
        Wallet wallet = requireLockedWallet(userId); // lock 1

        BigDecimal unitPrice = asset.getCurrentPrice(); // current price, not the purchase price
        BigDecimal proceeds = Money.cash(unitPrice.multiply(quantity));

        wallet.reduceHolding(asset, quantity); // throws before cash moves
        inventoryService.release(asset, quantity); // lock 2
        wallet.credit(proceeds);

        Transaction saved =
                transactionRepository.save(
                        Transaction.of(
                                wallet.getUser(), asset, TransactionType.SELL, quantity, unitPrice, proceeds));
        log.info(
                "SELL user={} asset={} qty={} unitPrice={} total={}",
                userId,
                asset.getSymbol(),
                quantity,
                unitPrice,
                proceeds);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> history(
            UUID userId,
            TransactionType type,
            String symbol,
            Instant from,
            Instant to,
            Pageable pageable) {
        Specification<Transaction> spec =
                Specification.allOf(
                        TransactionRepository.Specs.ownedBy(userId),
                        TransactionRepository.Specs.ofType(type),
                        TransactionRepository.Specs.forSymbol(symbol),
                        TransactionRepository.Specs.occurredFrom(from),
                        TransactionRepository.Specs.occurredUntil(to));
        return transactionRepository.findAll(spec, pageable);
    }

    private Wallet requireLockedWallet(UUID userId) {
        return walletRepository
                .findByUserIdForUpdate(userId)
                .orElseThrow(
                        () -> new NotFoundException("Wallet for user %s does not exist.".formatted(userId)));
    }

    private static void requirePositive(BigDecimal quantity) {
        if (!Money.isPositive(quantity)) {
            throw new BusinessRuleException("Quantity must be greater than zero.");
        }
    }
}
