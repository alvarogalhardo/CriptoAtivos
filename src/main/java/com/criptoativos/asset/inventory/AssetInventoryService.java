package com.criptoativos.asset.inventory;

import com.criptoativos.asset.Asset;
import com.criptoativos.asset.AssetService;
import com.criptoativos.common.exception.NotFoundException;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetInventoryService {

    private final AssetInventoryRepository inventoryRepository;
    private final AssetService assetService;

    public AssetInventoryService(
            AssetInventoryRepository inventoryRepository, AssetService assetService) {
        this.inventoryRepository = inventoryRepository;
        this.assetService = assetService;
    }

    /** Called inside the trading transaction, after the wallet lock is held. */
    @Transactional
    public void reserve(Asset asset, BigDecimal quantity) {
        lockedFor(asset).reserve(quantity);
    }

    @Transactional
    public void release(Asset asset, BigDecimal quantity) {
        lockedFor(asset).release(quantity);
    }

    @Transactional
    public AssetInventory restock(String symbol, BigDecimal quantity) {
        Asset asset = assetService.requireBySymbol(symbol); // 404 before any inventory error
        AssetInventory inventory = lockedFor(asset);
        inventory.restock(quantity);
        return inventory;
    }

    @Transactional(readOnly = true)
    public AssetInventory requireBySymbol(String symbol) {
        Asset asset = assetService.requireBySymbol(symbol);
        return inventoryRepository.findById(asset.getId()).orElseThrow(() -> missing(asset));
    }

    @Transactional(readOnly = true)
    public BigDecimal available(String symbol) {
        return requireBySymbol(symbol).getAvailableQuantity();
    }

    private AssetInventory lockedFor(Asset asset) {
        return inventoryRepository
                .findByAssetIdForUpdate(asset.getId())
                .orElseThrow(() -> missing(asset));
    }

    private static NotFoundException missing(Asset asset) {
        return new NotFoundException(
                "No inventory record for asset %s.".formatted(asset.getSymbol()));
    }
}
