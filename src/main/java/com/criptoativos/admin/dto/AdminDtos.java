package com.criptoativos.admin.dto;

import com.criptoativos.common.Money;
import com.criptoativos.user.Role;
import com.criptoativos.user.User;
import com.criptoativos.wallet.Wallet;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class AdminDtos {

    private AdminDtos() {}

    public record ChangeRoleRequest(@NotNull Role role) {}

    /**
     * An administrative view of an account.
     *
     * <p>The CPF is masked: an admin list is a common place to leak PII, and the last five digits are
     * enough to identify a record without exposing the document.
     */
    public record AdminUserResponse(
            UUID id,
            String name,
            String email,
            String cpfMasked,
            String role,
            boolean twoFactorEnabled,
            BigDecimal cashBalance,
            int holdingCount,
            Instant createdAt) {

        public static AdminUserResponse from(User user, int holdingCount) {
            Wallet wallet = user.getWallet();
            return new AdminUserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    mask(user.getCpf()),
                    user.getRole().name(),
                    user.isTwoFactorEnabled(),
                    wallet == null ? Money.ZERO_CASH : wallet.getCashBalance(),
                    holdingCount,
                    user.getCreatedAt());
        }

        /** {@code 52998224725} becomes {@code ***.***.247-25}. */
        private static String mask(String cpf) {
            if (cpf == null || cpf.length() != 11) {
                return "***.***.***-**";
            }
            return "***.***.%s-%s".formatted(cpf.substring(6, 9), cpf.substring(9, 11));
        }
    }
}
