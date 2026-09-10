package com.criptoativos.asset;

import com.criptoativos.asset.dto.AssetDtos.AssetResponse;
import com.criptoativos.asset.dto.AssetDtos.CreateCryptoAssetRequest;
import com.criptoativos.asset.dto.AssetDtos.UpdatePriceRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public Page<AssetResponse> list(
            @PageableDefault(size = 20, sort = "symbol", direction = Sort.Direction.ASC)
                    Pageable pageable) {
        return assetService.list(pageable).map(AssetResponse::from);
    }

    @GetMapping("/{symbol}")
    public AssetResponse bySymbol(@PathVariable String symbol) {
        return AssetResponse.from(assetService.requireBySymbol(symbol));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse create(@Valid @RequestBody CreateCryptoAssetRequest request) {
        return AssetResponse.from(assetService.createCrypto(request));
    }

    @PatchMapping("/{symbol}/price")
    @PreAuthorize("hasRole('ADMIN')")
    public AssetResponse updatePrice(
            @PathVariable String symbol, @Valid @RequestBody UpdatePriceRequest request) {
        return AssetResponse.from(assetService.updatePrice(symbol, request.price()));
    }
}
