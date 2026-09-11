package com.criptoativos.asset.inventory.dto;

import com.criptoativos.asset.inventory.AssetInventory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public final class InventoryDtos {

    private InventoryDtos() {}

    public record RestockRequest(
            @NotNull @DecimalMin(value = "0", message = "must not be negative")
                    BigDecimal availableQuantity) {}

    public record InventoryResponse(
            String symbol, BigDecimal availableQuantity, Instant updatedAt) {

        public static InventoryResponse from(AssetInventory inventory) {
            return new InventoryResponse(
                    inventory.getAsset().getSymbol(),
                    inventory.getAvailableQuantity(),
                    inventory.getUpdatedAt());
        }
    }
}
