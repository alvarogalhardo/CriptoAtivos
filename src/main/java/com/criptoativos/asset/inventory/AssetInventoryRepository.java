package com.criptoativos.asset.inventory;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetInventoryRepository extends JpaRepository<AssetInventory, UUID> {

    /**
     * Row-level lock for the trading path.
     *
     * <p>Callers must already hold the wallet lock — the order is always wallet then inventory, or
     * two concurrent trades on opposite assets can deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from AssetInventory i where i.assetId = :assetId")
    Optional<AssetInventory> findByAssetIdForUpdate(@Param("assetId") UUID assetId);
}
