package com.criptoativos.asset.inventory;

import com.criptoativos.asset.Asset;
import com.criptoativos.common.Money;
import com.criptoativos.common.exception.BusinessRuleException;
import com.criptoativos.common.exception.InsufficientInventoryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The exchange's available supply of an asset.
 *
 * <p>This is the {@code EstoqueCriptoAtivos} concept from the original design, which had a Java
 * class but no table and so never actually constrained anything. Without it the API silently
 * assumes infinite liquidity.
 *
 * <p>Shares its primary key with {@code assets} via {@link MapsId}, so there is exactly one
 * inventory row per asset and no separate id to keep in sync.
 */
@Entity
@Table(name = "asset_inventory")
public class AssetInventory {

    @Id
    @Column(name = "asset_id")
    private UUID assetId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "available_quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal availableQuantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version private long version;

    protected AssetInventory() {} // JPA

    public static AssetInventory forAsset(Asset asset, BigDecimal initialQuantity) {
        AssetInventory inventory = new AssetInventory();
        inventory.asset = asset;
        inventory.availableQuantity = Money.units(initialQuantity);
        return inventory;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    /** Removes quantity from available supply, for a buy. */
    public void reserve(BigDecimal quantity) {
        requirePositive(quantity);
        BigDecimal rounded = Money.units(quantity);
        if (availableQuantity.compareTo(rounded) < 0) {
            throw new InsufficientInventoryException(
                    "Only %s %s available, requested %s."
                            .formatted(availableQuantity, asset.getSymbol(), rounded));
        }
        this.availableQuantity = Money.units(availableQuantity.subtract(rounded));
    }

    /** Returns quantity to available supply, for a sell. */
    public void release(BigDecimal quantity) {
        requirePositive(quantity);
        this.availableQuantity = Money.units(availableQuantity.add(quantity));
    }

    public void restock(BigDecimal newQuantity) {
        if (newQuantity == null || newQuantity.signum() < 0) {
            throw new BusinessRuleException("Available quantity cannot be negative.");
        }
        this.availableQuantity = Money.units(newQuantity);
    }

    private static void requirePositive(BigDecimal value) {
        if (!Money.isPositive(value)) {
            throw new BusinessRuleException("Quantity must be greater than zero.");
        }
    }

    public UUID getAssetId() {
        return assetId;
    }

    public Asset getAsset() {
        return asset;
    }

    public BigDecimal getAvailableQuantity() {
        return availableQuantity;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
