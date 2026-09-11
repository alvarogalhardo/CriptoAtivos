package com.criptoativos.wallet;

import com.criptoativos.common.Money;
import com.criptoativos.wallet.dto.WalletDtos.HoldingResponse;
import com.criptoativos.wallet.dto.WalletDtos.WalletResponse;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Values a wallet at current prices.
 *
 * <p>Deliberately a pure function of the wallet it is given — no repository access — so it is fully
 * unit-testable without a database.
 */
@Service
public class PortfolioService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int PCT_SCALE = 4;

    public WalletResponse summarise(Wallet wallet) {
        List<HoldingResponse> holdings =
                wallet.getHoldings().stream()
                        .map(PortfolioService::valueHolding)
                        .sorted(Comparator.comparing(HoldingResponse::symbol))
                        .toList();

        BigDecimal invested =
                holdings.stream()
                        .map(HoldingResponse::investedValue)
                        .reduce(Money.ZERO_CASH, BigDecimal::add);
        BigDecimal market =
                holdings.stream()
                        .map(HoldingResponse::marketValue)
                        .reduce(Money.ZERO_CASH, BigDecimal::add);
        BigDecimal pnl = Money.cash(market.subtract(invested));

        return new WalletResponse(
                wallet.getId(),
                wallet.getCashBalance(),
                Money.cash(invested),
                Money.cash(market),
                Money.cash(wallet.getCashBalance().add(market)),
                pnl,
                percentage(pnl, invested),
                holdings);
    }

    private static HoldingResponse valueHolding(Holding holding) {
        BigDecimal quantity = holding.getQuantity();
        BigDecimal currentPrice = holding.getAsset().getCurrentPrice();
        BigDecimal invested = Money.cash(quantity.multiply(holding.getAverageCost()));
        BigDecimal market = Money.cash(quantity.multiply(currentPrice));
        BigDecimal pnl = Money.cash(market.subtract(invested));

        return new HoldingResponse(
                holding.getAsset().getSymbol(),
                holding.getAsset().getName(),
                quantity,
                holding.getAverageCost(),
                currentPrice,
                invested,
                market,
                pnl,
                percentage(pnl, invested));
    }

    /** Guards the zero-cost case explicitly: an empty portfolio must not divide by zero. */
    private static BigDecimal percentage(BigDecimal pnl, BigDecimal invested) {
        return invested.signum() == 0
                ? BigDecimal.ZERO.setScale(PCT_SCALE)
                : pnl.multiply(HUNDRED).divide(invested, PCT_SCALE, Money.ROUNDING);
    }
}
