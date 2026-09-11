package com.criptoativos.operation;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    /**
     * Filters for the history endpoint, composed so only the clauses actually supplied reach SQL.
     *
     * <p>A single query with {@code (:param is null or ...)} branches would be simpler to write,
     * but Postgres cannot infer the type of a null bind parameter (it fails with {@code function
     * upper(bytea) does not exist}), and the dead OR branches stop the planner using indexes.
     */
    final class Specs {

        private Specs() {}

        public static Specification<Transaction> ownedBy(UUID userId) {
            return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
        }

        public static Specification<Transaction> ofType(TransactionType type) {
            return type == null
                    ? null
                    : (root, query, cb) -> cb.equal(root.get("transactionType"), type);
        }

        public static Specification<Transaction> forSymbol(String symbol) {
            return symbol == null || symbol.isBlank()
                    ? null
                    : (root, query, cb) ->
                            cb.equal(
                                    cb.upper(root.get("asset").get("symbol")),
                                    symbol.toUpperCase());
        }

        public static Specification<Transaction> occurredFrom(Instant from) {
            return from == null
                    ? null
                    : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
        }

        public static Specification<Transaction> occurredUntil(Instant to) {
            return to == null
                    ? null
                    : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to);
        }
    }
}
