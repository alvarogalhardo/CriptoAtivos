package com.criptoativos.operation;

import com.criptoativos.common.Money;
import com.criptoativos.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

/** Cash moving in or out of a wallet. */
@Entity
@DiscriminatorValue("CASH")
public class CashOperation extends Operation {

    @Enumerated(EnumType.STRING)
    @Column(name = "cash_type", length = 10)
    private CashOperationType cashType;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    protected CashOperation() {} // JPA

    private static CashOperation of(User user, CashOperationType type, BigDecimal amount) {
        CashOperation operation = new CashOperation();
        BigDecimal rounded = Money.cash(amount);
        operation.initialise(user, "%s of %s".formatted(type, rounded));
        operation.cashType = type;
        operation.amount = rounded;
        return operation;
    }

    public static CashOperation deposit(User user, BigDecimal amount) {
        return of(user, CashOperationType.DEPOSIT, amount);
    }

    public static CashOperation withdrawal(User user, BigDecimal amount) {
        return of(user, CashOperationType.WITHDRAWAL, amount);
    }

    @Override
    public String summary() {
        return "%s of %s".formatted(cashType, amount);
    }

    public CashOperationType getCashType() {
        return cashType;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
