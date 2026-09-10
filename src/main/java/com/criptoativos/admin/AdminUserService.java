package com.criptoativos.admin;

import com.criptoativos.admin.dto.AdminDtos.AdminUserResponse;
import com.criptoativos.common.exception.BusinessRuleException;
import com.criptoativos.common.exception.NotFoundException;
import com.criptoativos.user.Role;
import com.criptoativos.user.User;
import com.criptoativos.user.UserRepository;
import com.criptoativos.wallet.Wallet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administrative operations on accounts.
 *
 * <p>The original console app exposed "list users" and "delete user" to anyone at the keyboard with
 * no guardrails. Here they are admin-only and refuse the three ways an administrator can lock
 * everyone out or destroy value by accident.
 *
 * <p>Responses are mapped inside the transaction. Returning entities and mapping in the controller
 * would throw {@code LazyInitializationException}, because {@code open-in-view} is disabled.
 */
@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> list(String search, Pageable pageable) {
        Page<User> users =
                search == null || search.isBlank()
                        ? userRepository.findAllWithWallet(pageable)
                        : userRepository.searchBy(search.trim(), pageable);

        Map<UUID, Integer> holdingCounts = holdingCountsFor(users.getContent());
        return users.map(
                user -> AdminUserResponse.from(user, holdingCounts.getOrDefault(user.getId(), 0)));
    }

    @Transactional(readOnly = true)
    public AdminUserResponse view(UUID id) {
        User user = get(id);
        return AdminUserResponse.from(user, holdingCountsFor(List.of(user)).getOrDefault(id, 0));
    }

    @Transactional(readOnly = true)
    public User get(UUID id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("User %s does not exist.".formatted(id)));
    }

    @Transactional
    public void delete(UUID actorId, UUID targetId, boolean force) {
        if (actorId.equals(targetId)) {
            throw new BusinessRuleException("You cannot delete your own account.");
        }
        User target = get(targetId);
        if (target.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessRuleException("The last administrator cannot be deleted.");
        }
        Wallet wallet = target.getWallet();
        boolean holdsValue =
                wallet != null
                        && (wallet.getCashBalance().signum() > 0 || !wallet.getHoldings().isEmpty());
        if (holdsValue && !force) {
            throw new BusinessRuleException(
                    "User has a non-zero balance or open holdings. Re-send with force=true to delete anyway.");
        }
        userRepository.delete(target); // cascades to wallet, holdings, operations, recovery codes
        log.warn("ADMIN DELETE actor={} target={} force={}", actorId, targetId, force);
    }

    @Transactional
    public AdminUserResponse changeRole(UUID actorId, UUID targetId, Role role) {
        User target = get(targetId);
        if (target.getRole() == Role.ADMIN
                && role != Role.ADMIN
                && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessRuleException("The last administrator cannot be demoted.");
        }
        target.assignRole(role);
        log.warn("ADMIN ROLE CHANGE actor={} target={} newRole={}", actorId, targetId, role);
        return AdminUserResponse.from(
                target, holdingCountsFor(List.of(target)).getOrDefault(targetId, 0));
    }

    /** One grouped query per page instead of a lazy collection load per user. */
    private Map<UUID, Integer> holdingCountsFor(List<User> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = users.stream().map(User::getId).toList();
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : userRepository.countHoldingsByUserIds(ids)) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }
}
