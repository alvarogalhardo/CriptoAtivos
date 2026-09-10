package com.criptoativos.asset.inventory;

import com.criptoativos.asset.inventory.dto.InventoryDtos.InventoryResponse;
import com.criptoativos.asset.inventory.dto.InventoryDtos.RestockRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets/{symbol}/inventory")
public class AssetInventoryController {

    private final AssetInventoryService inventoryService;

    public AssetInventoryController(AssetInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public InventoryResponse get(@PathVariable String symbol) {
        return InventoryResponse.from(inventoryService.requireBySymbol(symbol));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse restock(
            @PathVariable String symbol, @Valid @RequestBody RestockRequest request) {
        return InventoryResponse.from(inventoryService.restock(symbol, request.availableQuantity()));
    }
}
