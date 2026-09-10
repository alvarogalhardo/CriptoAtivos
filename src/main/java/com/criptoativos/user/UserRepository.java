package com.criptoativos.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCpf(String cpf);

    long countByRole(Role role);

    /**
     * Name or email contains {@code search}.
     *
     * <p>The parameter is never null — callers with no search term use {@code findAll} instead.
     * Postgres cannot infer the type of a null bind parameter inside {@code lower(concat(...))} and
     * fails with {@code function lower(bytea) does not exist}.
     */
    @EntityGraph(attributePaths = "wallet")
    @Query(
            """
            select u from User u
            where lower(u.email) like lower(concat('%', :search, '%'))
               or lower(u.name) like lower(concat('%', :search, '%'))
            """)
    Page<User> searchBy(@Param("search") String search, Pageable pageable);

    /**
     * Admin listing. The wallet is a to-one association, so fetching it in the graph is safe with
     * pagination; fetching the holdings collection here would force Hibernate to paginate in memory.
     * Holding counts come from {@code countHoldingsByUserIds} in one extra query instead.
     */
    @EntityGraph(attributePaths = "wallet")
    @Query("select u from User u")
    Page<User> findAllWithWallet(Pageable pageable);

    /** One grouped query for a whole page of users, rather than a lazy load per row. */
    @Query(
            """
            select h.wallet.user.id, count(h)
            from Holding h
            where h.wallet.user.id in :userIds
            group by h.wallet.user.id
            """)
    List<Object[]> countHoldingsByUserIds(@Param("userIds") Collection<UUID> userIds);
}
