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

    public WalletService(WalletRepository walletRepository, OperationRepository operationRepository) {
        this.walletRepository = walletRepository;
        this.operationRepository = operationRepository;
    }

    @Transactional(readOnly = true)
    public Wallet requireByUserId(UUID userId) {
        return walletRepository.findByUserId(userId).orElseThrow(() -> missing(userId));
    }

    @Transactional
    public Wallet deposit(UUID userId, BigDecimal amount) {
        Wallet wallet = requireLocked(userId);
        wallet.credit(amount);
        operationRepository.save(CashOperation.deposit(wallet.getUser(), amount));
        log.info("DEPOSIT user={} amount={} balance={}", userId, amount, wallet.getCashBalance());
        return wallet;
    }

    @Transactional
    public Wallet withdraw(UUID userId, BigDecimal amount) {
        Wallet wallet = requireLocked(userId);
        wallet.debit(amount); // throws before anything is recorded
        operationRepository.save(CashOperation.withdrawal(wallet.getUser(), amount));
        log.info("WITHDRAWAL user={} amount={} balance={}", userId, amount, wallet.getCashBalance());
        return wallet;
    }

    private Wallet requireLocked(UUID userId) {
        return walletRepository.findByUserIdForUpdate(userId).orElseThrow(() -> missing(userId));
    }

    private static NotFoundException missing(UUID userId) {
        return new NotFoundException("Wallet for user %s does not exist.".formatted(userId));
    }
}
