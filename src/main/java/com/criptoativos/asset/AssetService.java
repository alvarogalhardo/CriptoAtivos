package com.criptoativos.asset;

import com.criptoativos.asset.dto.AssetDtos.CreateCryptoAssetRequest;
import com.criptoativos.asset.inventory.AssetInventory;
import com.criptoativos.asset.inventory.AssetInventoryRepository;
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
    private final AssetInventoryRepository inventoryRepository;

    public AssetService(
            AssetRepository assetRepository, AssetInventoryRepository inventoryRepository) {
        this.assetRepository = assetRepository;
        this.inventoryRepository = inventoryRepository;
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
        CryptoAsset saved = assetRepository.save(asset);
        // An asset with no inventory row would blow up on its first buy.
        inventoryRepository.save(AssetInventory.forAsset(saved, BigDecimal.ZERO));
        return saved;
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
