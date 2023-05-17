package com.iy.inventoryservice.service;

import com.iy.inventoryservice.dto.InventoryResponse;
import com.iy.inventoryservice.model.Inventory;
import com.iy.inventoryservice.repository.InventoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> skuCodes) {


        List<Inventory> inventories = inventoryRepository.findBySkuCodeIn(skuCodes);

        return inventories
            .stream()
            .map(inventory ->
                     InventoryResponse
                         .builder()
                         .skuCode(inventory.getSkuCode())
                         .isInStock(inventory.getQuantity() > 0)
                         .build()
            ).toList();
    }
}
