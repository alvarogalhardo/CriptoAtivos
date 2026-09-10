package com.criptoativos.wallet.dto;

import com.criptoativos.wallet.Holding;
import com.criptoativos.wallet.Wallet;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class WalletDtos {

    private WalletDtos() {}

    public record CashAmountRequest(
            @NotNull @DecimalMin(value = "0.01", message = "must be greater than zero")
                    BigDecimal amount) {}

    public record HoldingResponse(
            String symbol, String name, BigDecimal quantity, BigDecimal averageCost) {

        public static HoldingResponse from(Holding holding) {
            return new HoldingResponse(
                    holding.getAsset().getSymbol(),
                    holding.getAsset().getName(),
                    holding.getQuantity(),
                    holding.getAverageCost());
        }
    }

    public record WalletResponse(UUID id, BigDecimal cashBalance, List<HoldingResponse> holdings) {

        public static WalletResponse from(Wallet wallet) {
            List<HoldingResponse> holdings =
                    wallet.getHoldings().stream()
                            .map(HoldingResponse::from)
                            .sorted(Comparator.comparing(HoldingResponse::symbol))
                            .toList();
            return new WalletResponse(wallet.getId(), wallet.getCashBalance(), holdings);
        }
    }
}
