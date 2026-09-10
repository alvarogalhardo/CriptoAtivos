package com.criptoativos.operation;

import com.criptoativos.asset.Asset;
import com.criptoativos.common.Money;
import com.criptoativos.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

/**
 * A completed buy or sell.
 *
 * <p>The executed unit price and total are stored, not recomputed from today's price: this is a
 * permanent record of what actually happened. The original design read the price back off the asset
 * object, so history changed whenever the price did.
 */
@Entity
@DiscriminatorValue("TRANSACTION")
public class Transaction extends Operation {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 10)
    private TransactionType transactionType;

    @Column(precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 19, scale = 8)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    protected Transaction() {} // JPA

    public static Transaction of(
            User user,
            Asset asset,
            TransactionType type,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount) {
        Transaction transaction = new Transaction();
        transaction.asset = asset;
        transaction.transactionType = type;
        transaction.quantity = Money.units(quantity);
        transaction.unitPrice = Money.units(unitPrice);
        transaction.totalAmount = Money.cash(totalAmount);
        transaction.initialise(
                user,
                "%s %s %s @ %s"
                        .formatted(type, transaction.quantity, asset.getSymbol(), transaction.unitPrice));
        return transaction;
    }

    @Override
    public String summary() {
        return "%s of %s %s at %s (total %s)"
                .formatted(transactionType, quantity, asset.getSymbol(), unitPrice, totalAmount);
    }

    public Asset getAsset() {
        return asset;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
