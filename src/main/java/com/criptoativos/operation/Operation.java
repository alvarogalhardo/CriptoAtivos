package com.criptoativos.operation;

import com.criptoativos.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Anything a user did that is worth an immutable record.
 *
 * <p>The original design had this abstract class with a single subclass, which made the abstraction
 * dead weight. It now has two real ones — {@link Transaction} for asset trades and {@link
 * CashOperation} for deposits and withdrawals — so the hierarchy earns its place.
 */
@Entity
@Table(name = "operations")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "operation_type",
        discriminatorType = DiscriminatorType.STRING,
        length = 31)
public abstract class Operation {

    @Id @GeneratedValue private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected Operation() {} // JPA

    protected void initialise(User user, String description) {
        this.user = user;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        if (this.occurredAt == null) {
            this.occurredAt = Instant.now();
        }
    }

    /**
     * A human-readable summary. Replaces {@code executarOperacao()}, which printed to stdout and
     * returned nothing usable.
     */
    public abstract String summary();

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getDescription() {
        return description;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
