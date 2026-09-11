package com.criptoativos.user;

import com.criptoativos.wallet.Wallet;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id @GeneratedValue private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled;

    /** AES ciphertext of the Base32 TOTP secret — never the raw secret. */
    @Column(name = "two_factor_secret", length = 255)
    private String twoFactorSecret;

    @Column(name = "two_factor_confirmed_at")
    private Instant twoFactorConfirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Wallet wallet;

    protected User() {} // JPA

    private User(String name, String email, String passwordHash, String cpf, Role role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.cpf = cpf;
        this.role = role;
    }

    /**
     * Creates a user together with the empty wallet every account must have. The original design
     * left this coupling implicit, so a user could exist with no wallet at all.
     */
    public static User create(
            String name, String email, String passwordHash, String cpf, Role role) {
        User user = new User(name, email, passwordHash, cpf, role);
        user.wallet = Wallet.forUser(user);
        return user;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void rename(String name) {
        this.name = name;
    }

    public void assignRole(Role role) {
        this.role = role;
    }

    /** Stores a secret that is not yet active: 2FA turns on only once a code is confirmed. */
    public void stageTwoFactorSecret(String encryptedSecret) {
        this.twoFactorSecret = encryptedSecret;
        this.twoFactorEnabled = false;
        this.twoFactorConfirmedAt = null;
    }

    public void confirmTwoFactor() {
        this.twoFactorEnabled = true;
        this.twoFactorConfirmedAt = Instant.now();
    }

    public void disableTwoFactor() {
        this.twoFactorEnabled = false;
        this.twoFactorSecret = null;
        this.twoFactorConfirmedAt = null;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getCpf() {
        return cpf;
    }

    public Role getRole() {
        return role;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }

    public Instant getTwoFactorConfirmedAt() {
        return twoFactorConfirmedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Wallet getWallet() {
        return wallet;
    }
}
