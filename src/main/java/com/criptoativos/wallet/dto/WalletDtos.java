package com.criptoativos.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class WalletDtos {

    private WalletDtos() {}

    public record CashAmountRequest(
            @NotNull @DecimalMin(value = "0.01", message = "must be greater than zero")
                    BigDecimal amount) {}

    /** One position, valued at the asset's current price. */
    public record HoldingResponse(
            String symbol,
            String name,
            BigDecimal quantity,
            BigDecimal averageCost,
            BigDecimal currentPrice,
            BigDecimal investedValue,
            BigDecimal marketValue,
            BigDecimal unrealisedPnl,
            BigDecimal unrealisedPnlPct) {}

    /**
     * Cash plus positions.
     *
     * <p>{@code investedValue} is what the holdings cost, {@code marketValue} what they are worth
     * now, and {@code totalValue} includes uninvested cash.
     */
    public record WalletResponse(
            UUID id,
            BigDecimal cashBalance,
            BigDecimal investedValue,
            BigDecimal marketValue,
            BigDecimal totalValue,
            BigDecimal unrealisedPnl,
            BigDecimal unrealisedPnlPct,
            List<HoldingResponse> holdings) {}
}
