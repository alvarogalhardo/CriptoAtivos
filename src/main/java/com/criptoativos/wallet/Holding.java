package com.criptoativos.wallet;

import com.criptoativos.asset.Asset;
import com.criptoativos.common.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * A quantity of one asset held in one wallet.
 *
 * <p>Replaces the original {@code HashMap<CriptoAtivo, Double>}, which had no {@code equals}/{@code
 * hashCode} on the key — so buying the same coin twice produced two unrelated entries. The unique
 * constraint on (wallet, asset) makes that impossible at the database level.
 */
@Entity
@Table(
        name = "holdings",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_holdings_wallet_asset",
                        columnNames = {"wallet_id", "asset_id"}))
public class Holding {

    @Id @GeneratedValue private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "average_cost", nullable = false, precision = 19, scale = 8)
    private BigDecimal averageCost;

    @Version private long version;

    protected Holding() {} // JPA

    static Holding open(Wallet wallet, Asset asset, BigDecimal quantity, BigDecimal unitPrice) {
        Holding holding = new Holding();
        holding.wallet = wallet;
        holding.asset = asset;
        holding.quantity = Money.units(quantity);
        holding.averageCost = Money.units(unitPrice);
        return holding;
    }

    /** Adds quantity and recomputes the weighted average cost basis. */
    void increase(BigDecimal addedQuantity, BigDecimal unitPrice) {
        BigDecimal existingCost = this.quantity.multiply(this.averageCost);
        BigDecimal addedCost = addedQuantity.multiply(unitPrice);
        BigDecimal newQuantity = this.quantity.add(addedQuantity);
        this.averageCost =
                Money.units(
                        existingCost
                                .add(addedCost)
                                .divide(newQuantity, Money.UNIT_SCALE, Money.ROUNDING));
        this.quantity = Money.units(newQuantity);
    }

    /** Removes quantity. Average cost is unchanged: selling does not alter the cost basis. */
    void decrease(BigDecimal soldQuantity) {
        this.quantity = Money.units(this.quantity.subtract(soldQuantity));
    }

    public UUID getId() {
        return id;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Asset getAsset() {
        return asset;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getAverageCost() {
        return averageCost;
    }
}
