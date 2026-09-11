package com.criptoativos.asset.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.criptoativos.asset.Asset;
import com.criptoativos.asset.CryptoAsset;
import com.criptoativos.common.exception.BusinessRuleException;
import com.criptoativos.common.exception.InsufficientInventoryException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AssetInventoryTest {

    private static Asset btc() {
        return CryptoAsset.create("BTC", "Bitcoin", null, new BigDecimal("1000"), "bitcoin");
    }

    private static AssetInventory inventoryOf(String quantity) {
        return AssetInventory.forAsset(btc(), new BigDecimal(quantity));
    }

    @Test
    void reserveReducesAvailableSupply() {
        AssetInventory inventory = inventoryOf("10");

        inventory.reserve(new BigDecimal("3"));

        assertThat(inventory.getAvailableQuantity()).isEqualByComparingTo("7");
    }

    @Test
    void reserveBeyondSupplyThrowsAndChangesNothing() {
        AssetInventory inventory = inventoryOf("2");

        assertThatThrownBy(() -> inventory.reserve(new BigDecimal("5")))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Only")
                .hasMessageContaining("BTC");

        assertThat(inventory.getAvailableQuantity()).isEqualByComparingTo("2");
    }

    @Test
    void reserveThenReleaseIsARoundTrip() {
        AssetInventory inventory = inventoryOf("10");

        inventory.reserve(new BigDecimal("4"));
        inventory.release(new BigDecimal("4"));

        assertThat(inventory.getAvailableQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void reserveRejectsNonPositiveQuantities() {
        AssetInventory inventory = inventoryOf("10");

        assertThatThrownBy(() -> inventory.reserve(BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> inventory.reserve(new BigDecimal("-1")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reservingTheExactRemainingSupplyIsAllowed() {
        AssetInventory inventory = inventoryOf("2.5");

        inventory.reserve(new BigDecimal("2.5"));

        assertThat(inventory.getAvailableQuantity()).isEqualByComparingTo("0");
    }

    @Test
    void restockRejectsANegativeQuantity() {
        AssetInventory inventory = inventoryOf("10");

        assertThatThrownBy(() -> inventory.restock(new BigDecimal("-0.01")))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> inventory.restock(null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void restockAllowsZero() {
        AssetInventory inventory = inventoryOf("10");

        inventory.restock(BigDecimal.ZERO);

        assertThat(inventory.getAvailableQuantity()).isEqualByComparingTo("0");
    }
}
