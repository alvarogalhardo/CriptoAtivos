package com.criptoativos.wallet;

import com.criptoativos.common.exception.NotFoundException;
import com.criptoativos.operation.CashOperation;
import com.criptoativos.operation.OperationRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final OperationRepository operationRepository;

    public WalletService(
            WalletRepository walletRepository, OperationRepository operationRepository) {
        this.walletRepository = walletRepository;
        this.operationRepository = operationRepository;
    }

    /**
     * Read path: fetches holdings eagerly so the response can be mapped after the session closes.
     */
    @Transactional(readOnly = true)
    public Wallet requireByUserId(UUID userId) {
        return walletRepository.findWithHoldingsByUserId(userId).orElseThrow(() -> missing(userId));
    }

    @Transactional
    public Wallet deposit(UUID userId, BigDecimal amount) {
        Wallet wallet = requireLocked(userId);
        wallet.credit(amount);
        operationRepository.save(CashOperation.deposit(wallet.getUser(), amount));
        log.info("DEPOSIT user={} amount={} balance={}", userId, amount, wallet.getCashBalance());
        return withHoldings(userId);
    }

    @Transactional
    public Wallet withdraw(UUID userId, BigDecimal amount) {
        Wallet wallet = requireLocked(userId);
        wallet.debit(amount); // throws before anything is recorded
        operationRepository.save(CashOperation.withdrawal(wallet.getUser(), amount));
        log.info(
                "WITHDRAWAL user={} amount={} balance={}", userId, amount, wallet.getCashBalance());
        return withHoldings(userId);
    }

    /**
     * Re-reads through the entity graph so the returned wallet can be mapped to a response after
     * the transaction ends. Same persistence context, so this initialises the managed instance
     * rather than loading a second one.
     */
    private Wallet withHoldings(UUID userId) {
        return walletRepository.findWithHoldingsByUserId(userId).orElseThrow(() -> missing(userId));
    }

    private Wallet requireLocked(UUID userId) {
        return walletRepository.findByUserIdForUpdate(userId).orElseThrow(() -> missing(userId));
    }

    private static NotFoundException missing(UUID userId) {
        return new NotFoundException("Wallet for user %s does not exist.".formatted(userId));
    }
}
