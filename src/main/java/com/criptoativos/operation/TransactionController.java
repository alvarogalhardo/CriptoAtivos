package com.criptoativos.operation;

import com.criptoativos.common.SecurityUtils;
import com.criptoativos.operation.dto.TransactionDtos.TradeRequest;
import com.criptoativos.operation.dto.TransactionDtos.TransactionResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/buy")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse buy(@Valid @RequestBody TradeRequest request) {
        return TransactionResponse.from(
                transactionService.buy(
                        SecurityUtils.currentUserId(), request.symbol(), request.quantity()));
    }

    @PostMapping("/sell")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse sell(@Valid @RequestBody TradeRequest request) {
        return TransactionResponse.from(
                transactionService.sell(
                        SecurityUtils.currentUserId(), request.symbol(), request.quantity()));
    }

    @GetMapping
    public Page<TransactionResponse> history(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return transactionService
                .history(SecurityUtils.currentUserId(), type, symbol, from, to, pageable)
                .map(TransactionResponse::from);
    }
}
