package com.criptoativos.asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    Optional<Asset> findBySymbolIgnoreCase(String symbol);

    boolean existsBySymbolIgnoreCase(String symbol);

    /** Crypto assets that have a provider id, i.e. the ones the refresh job can quote. */
    @Query("select c from CryptoAsset c where c.externalId is not null")
    List<CryptoAsset> findTrackedCryptoAssets();
}
