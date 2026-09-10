package com.criptoativos.asset;

import com.criptoativos.common.Money;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@DiscriminatorValue("CRYPTO")
public class CryptoAsset extends Asset {

    @Column(name = "daily_change_pct", precision = 9, scale = 4)
    private BigDecimal dailyChangePct;

    /** Provider-side identifier, e.g. the CoinGecko coin id {@code "bitcoin"}. */
    @Column(name = "external_id", length = 60)
    private String externalId;

    @Column(name = "price_updated_at")
    private Instant priceUpdatedAt;

    protected CryptoAsset() {} // JPA

    private CryptoAsset(
            String symbol, String name, String description, BigDecimal price, String externalId) {
        super(symbol, name, description, price);
        this.externalId = externalId;
    }

    public static CryptoAsset create(
            String symbol, String name, String description, BigDecimal price, String externalId) {
        return new CryptoAsset(symbol, name, description, price, externalId);
    }

    @Override
    public void updatePrice(BigDecimal newPrice, BigDecimal dailyChangePct) {
        setCurrentPrice(newPrice);
        this.dailyChangePct = dailyChangePct == null ? null : dailyChangePct.setScale(4, Money.ROUNDING);
        this.priceUpdatedAt = Instant.now();
    }

    @Override
    public String category() {
        return "CRYPTO";
    }

    public BigDecimal getDailyChangePct() {
        return dailyChangePct;
    }

    public String getExternalId() {
        return externalId;
    }

    public Instant getPriceUpdatedAt() {
        return priceUpdatedAt;
    }
}
