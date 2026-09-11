package com.criptoativos.operation.dto;

import com.criptoativos.operation.Transaction;
import com.criptoativos.operation.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TransactionDtos {

    private TransactionDtos() {}

    public record TradeRequest(
            @NotBlank @Size(max = 20) String symbol,
            @NotNull @DecimalMin(value = "0.00000001", message = "must be greater than zero")
                    BigDecimal quantity) {}

    public record TransactionResponse(
            UUID id,
            String type,
            String symbol,
            String assetName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            Instant occurredAt) {

        public static TransactionResponse from(Transaction transaction) {
            return new TransactionResponse(
                    transaction.getId(),
                    transaction.getTransactionType().name(),
                    transaction.getAsset().getSymbol(),
                    transaction.getAsset().getName(),
                    transaction.getQuantity(),
                    transaction.getUnitPrice(),
                    transaction.getTotalAmount(),
                    transaction.getOccurredAt());
        }
    }

    /** Optional filters for the history endpoint. */
    public record HistoryFilter(TransactionType type, String symbol, Instant from, Instant to) {}
}
