package com.acme.report;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InventoryProjectionService {

    private final InventoryRepository inventoryRepository;

    public InventoryProjectionService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public Map<String, Integer> loadAvailableQuantities(List<String> productIds) {
        return inventoryRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(InventorySnapshot::productId, InventorySnapshot::availableQuantity));
    }
}
