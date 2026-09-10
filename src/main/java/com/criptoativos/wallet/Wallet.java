package com.criptoativos.wallet;

import com.criptoativos.common.Money;
import com.criptoativos.common.exception.BusinessRuleException;
import com.criptoativos.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A user's cash balance and (from Task 9) their asset holdings.
 *
 * <p>All balance invariants live here rather than in the service, so no caller can move money
 * without going through the checks.
 */
@Entity
@Table(name = "wallets")
public class Wallet {

    @Id @GeneratedValue private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "cash_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashBalance = Money.ZERO_CASH;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version private long version;

    protected Wallet() {} // JPA

    public static Wallet forUser(User user) {
        Wallet wallet = new Wallet();
        wallet.user = user;
        wallet.cashBalance = Money.ZERO_CASH;
        return wallet;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void credit(BigDecimal amount) {
        requirePositive(amount);
        this.cashBalance = Money.cash(this.cashBalance.add(amount));
    }

    public void debit(BigDecimal amount) {
        requirePositive(amount);
        BigDecimal rounded = Money.cash(amount);
        if (this.cashBalance.compareTo(rounded) < 0) {
            throw new BusinessRuleException(
                    "Insufficient funds: balance is %s, required %s.".formatted(this.cashBalance, rounded));
        }
        this.cashBalance = Money.cash(this.cashBalance.subtract(rounded));
    }

    private static void requirePositive(BigDecimal value) {
        if (!Money.isPositive(value)) {
            throw new BusinessRuleException("Amount must be greater than zero.");
        }
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
