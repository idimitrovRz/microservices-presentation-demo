package com.iy.orderservice.service;

import com.iy.orderservice.dto.InventoryResponse;
import com.iy.orderservice.dto.OrderLineItemsDto;
import com.iy.orderservice.dto.OrderRequest;
import com.iy.orderservice.model.Order;
import com.iy.orderservice.model.OrderLineItems;
import com.iy.orderservice.repository.OrderRepository;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClient;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItemsList = orderRequest.getOrderLineItemsDtoList()
                                                              .stream()
                                                              .map(this::mapToDto)
                                                              .toList();
        order.setOrderLineItemsList(orderLineItemsList);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                                     .map(OrderLineItems::getSkuCode)
                                     .toList();

        InventoryResponse[] inventoryResponses = webClient.build()
                                                          .get()
                                                          .uri("http://inventory-service/api/inventory",
                                                               uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build()
                                                          )
                                                          .retrieve()
                                                          .bodyToMono(InventoryResponse[].class)
                                                          .block();

        assert inventoryResponses != null;

        Boolean allProductsInStock = inventoryResponses.length > 0
                                     && inventoryResponses.length == order.getOrderLineItemsList().size()
                                     && Arrays.stream(inventoryResponses).allMatch(InventoryResponse::getIsInStock);

        if (Boolean.TRUE.equals(allProductsInStock)) {
            orderRepository.save(order);
            return "Order placed successfully";
        } else {
            throw new IllegalArgumentException("Product is out of stock");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
