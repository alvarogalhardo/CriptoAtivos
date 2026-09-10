package com.criptoativos.asset.price;

import com.criptoativos.asset.AssetRepository;
import com.criptoativos.asset.CryptoAsset;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Periodically refreshes prices for every asset that has a provider id. */
@Component
public class PriceRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(PriceRefreshJob.class);

    private final AssetRepository assetRepository;
    private final PriceProvider priceProvider;

    public PriceRefreshJob(AssetRepository assetRepository, PriceProvider priceProvider) {
        this.assetRepository = assetRepository;
        this.priceProvider = priceProvider;
    }

    @Scheduled(cron = "${app.prices.refresh-cron}")
    @Transactional
    public void refresh() {
        List<CryptoAsset> tracked = assetRepository.findTrackedCryptoAssets();
        if (tracked.isEmpty()) {
            return;
        }
        Map<String, PriceProvider.PriceQuote> quotes =
                priceProvider.fetchQuotes(tracked.stream().map(CryptoAsset::getExternalId).toList());

        int updated = 0;
        for (CryptoAsset asset : tracked) {
            PriceProvider.PriceQuote quote = quotes.get(asset.getExternalId());
            if (quote != null) {
                // An asset missing from the response keeps its previous price.
                asset.updatePrice(quote.price(), quote.dailyChangePct());
                updated++;
            }
        }
        log.info("Refreshed {} of {} tracked asset prices", updated, tracked.size());
    }
}
