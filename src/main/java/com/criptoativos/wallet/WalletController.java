package com.criptoativos.wallet;

import com.criptoativos.common.SecurityUtils;
import com.criptoativos.wallet.dto.WalletDtos.CashAmountRequest;
import com.criptoativos.wallet.dto.WalletDtos.WalletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller is always resolved from the token, never from a path variable, so one user cannot read
 * or move another user's money.
 */
@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public WalletResponse myWallet() {
        return WalletResponse.from(walletService.requireByUserId(SecurityUtils.currentUserId()));
    }

    @PostMapping("/deposits")
    public WalletResponse deposit(@Valid @RequestBody CashAmountRequest request) {
        return WalletResponse.from(
                walletService.deposit(SecurityUtils.currentUserId(), request.amount()));
    }

    @PostMapping("/withdrawals")
    public WalletResponse withdraw(@Valid @RequestBody CashAmountRequest request) {
        return WalletResponse.from(
                walletService.withdraw(SecurityUtils.currentUserId(), request.amount()));
    }
}
