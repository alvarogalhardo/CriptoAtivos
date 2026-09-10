package com.criptoativos.asset;

import com.criptoativos.asset.dto.AssetDtos.CreateCryptoAssetRequest;
import com.criptoativos.common.exception.ConflictException;
import com.criptoativos.common.exception.NotFoundException;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetService {

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Transactional(readOnly = true)
    public Page<Asset> list(Pageable pageable) {
        return assetRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Asset requireBySymbol(String symbol) {
        return assetRepository
                .findBySymbolIgnoreCase(symbol)
                .orElseThrow(
                        () -> new NotFoundException("Asset %s does not exist.".formatted(symbol.toUpperCase())));
    }

    @Transactional
    public CryptoAsset createCrypto(CreateCryptoAssetRequest request) {
        if (assetRepository.existsBySymbolIgnoreCase(request.symbol())) {
            throw new ConflictException(
                    "Asset %s already exists.".formatted(request.symbol().toUpperCase()));
        }
        CryptoAsset asset =
                CryptoAsset.create(
                        request.symbol(),
                        request.name(),
                        request.description(),
                        request.currentPrice(),
                        request.externalId());
        return assetRepository.save(asset);
    }

    /**
     * Sets a price directly. This is what makes the API usable with {@code
     * app.prices.provider=manual} and no network, and it is how the tests control prices.
     */
    @Transactional
    public Asset updatePrice(String symbol, BigDecimal price) {
        Asset asset = requireBySymbol(symbol);
        asset.updatePrice(price, null);
        return asset;
    }
}
