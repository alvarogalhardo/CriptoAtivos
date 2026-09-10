package com.criptoativos.asset.dto;

import com.criptoativos.asset.Asset;
import com.criptoativos.asset.CryptoAsset;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class AssetDtos {

    private AssetDtos() {}

    public record CreateCryptoAssetRequest(
            @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9]+") String symbol,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            @NotNull @DecimalMin("0") BigDecimal currentPrice,
            @Size(max = 60) String externalId) {}

    public record UpdatePriceRequest(@NotNull @DecimalMin("0") BigDecimal price) {}

    public record AssetResponse(
            UUID id,
            String symbol,
            String name,
            String description,
            String category,
            BigDecimal currentPrice,
            BigDecimal dailyChangePct,
            Instant priceUpdatedAt) {

        public static AssetResponse from(Asset asset) {
            BigDecimal change = null;
            Instant updated = null;
            if (asset instanceof CryptoAsset crypto) {
                change = crypto.getDailyChangePct();
                updated = crypto.getPriceUpdatedAt();
            }
            return new AssetResponse(
                    asset.getId(),
                    asset.getSymbol(),
                    asset.getName(),
                    asset.getDescription(),
                    asset.category(),
                    asset.getCurrentPrice(),
                    change,
                    updated);
        }
    }
}
