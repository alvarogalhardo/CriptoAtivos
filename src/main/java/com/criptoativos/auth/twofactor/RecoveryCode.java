package com.criptoativos.auth.twofactor;

import com.criptoativos.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A single-use backup code, stored hashed exactly like a password. */
@Entity
@Table(name = "user_recovery_codes")
public class RecoveryCode {

    @Id @GeneratedValue private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code_hash", nullable = false, length = 72)
    private String codeHash;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RecoveryCode() {} // JPA

    public static RecoveryCode issue(User user, String codeHash) {
        RecoveryCode code = new RecoveryCode();
        code.user = user;
        code.codeHash = codeHash;
        return code;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
