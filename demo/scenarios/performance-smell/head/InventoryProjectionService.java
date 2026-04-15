package com.acme.report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryProjectionService {

    private final InventoryRepository inventoryRepository;

    public InventoryProjectionService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public Map<String, Integer> loadAvailableQuantities(List<String> productIds) {
        Map<String, Integer> quantities = new HashMap<>();
        for (String productId : productIds) {
            quantities.put(
                    productId,
                    inventoryRepository.findById(productId).orElseThrow().availableQuantity()
            );
        }
        return quantities;
    }
}
