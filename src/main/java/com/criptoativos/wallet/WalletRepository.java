package com.criptoativos.wallet;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUserId(UUID userId);

    /**
     * Read path for the API.
     *
     * <p>{@code open-in-view} is off, so the Hibernate session closes when the service method
     * returns. Without this graph, mapping the response DTO in the controller would throw
     * {@code LazyInitializationException} on {@code wallet.holdings}.
     */
    @EntityGraph(attributePaths = {"holdings", "holdings.asset"})
    @Query("select w from Wallet w where w.user.id = :userId")
    Optional<Wallet> findWithHoldingsByUserId(@Param("userId") UUID userId);

    /**
     * Row-level lock so concurrent operations on the same wallet serialise instead of racing.
     *
     * <p>Money is the one place where retrying on an optimistic conflict is worse than simply
     * waiting. Callers that also need an inventory lock must take this one first.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.user.id = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") UUID userId);
}
