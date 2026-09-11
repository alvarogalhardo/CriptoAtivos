package com.criptoativos.asset;

import com.criptoativos.common.Money;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A tradeable asset.
 *
 * <p>The abstract base is inherited from the original academic design and kept deliberately: single
 * table inheritance turns it into a real extension point, so a future {@code StockAsset} needs no
 * schema change beyond nullable columns.
 */
@Entity
@Table(name = "assets")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "asset_type", discriminatorType = DiscriminatorType.STRING, length = 31)
public abstract class Asset {

    @Id @GeneratedValue private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "current_price", nullable = false, precision = 19, scale = 8)
    private BigDecimal currentPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version private long version;

    protected Asset() {} // JPA

    protected Asset(String symbol, String name, String description, BigDecimal currentPrice) {
        this.symbol = symbol.toUpperCase();
        this.name = name;
        this.description = description;
        this.currentPrice = Money.units(currentPrice);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    /**
     * Applies a fresh quote. Replaces the original {@code calcularVariacaoDiaria()}, which only
     * printed a message and computed nothing.
     */
    public abstract void updatePrice(BigDecimal newPrice, BigDecimal dailyChangePct);

    /** Discriminator exposed to clients, e.g. {@code "CRYPTO"}. */
    public abstract String category();

    protected void setCurrentPrice(BigDecimal price) {
        this.currentPrice = Money.units(price);
    }

    public UUID getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
