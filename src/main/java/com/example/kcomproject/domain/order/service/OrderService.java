package com.example.kcomproject.domain.order.service;

import com.example.kcomproject.domain.order.dto.request.OrderRequest;
import com.example.kcomproject.domain.order.dto.response.OrderResponse;
import com.example.kcomproject.domain.order.entity.Order;
import com.example.kcomproject.domain.order.entity.OrderItem;
import com.example.kcomproject.domain.order.entity.OrderStatus;
import com.example.kcomproject.domain.order.repository.OrderItemRepository;
import com.example.kcomproject.domain.order.repository.OrderRepository;
import com.example.kcomproject.global.dto.PageResponseDto;
import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.domain.OrderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderTransactionService orderTransactionService;
    private final OrderFacade orderFacade;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public PageResponseDto<OrderResponse> getOrders(
            Long userId, LocalDateTime startDate, LocalDateTime endDate, 
            List<OrderStatus> statuses, Long lastId, int size) {
        
        List<Order> orders = orderRepository.findOrdersByFilter(userId, startDate, endDate, statuses, lastId, size);

        boolean hasNext = orders.size() > size;
        List<Order> content = hasNext ? orders.subList(0, size) : orders;
        
        List<OrderResponse> dtoList = content.stream()
                .map(order -> OrderResponse.builder()
                        .orderId(order.getId())
                        .userId(order.getUserId())
                        .storeId(order.getStoreId())
                        .totalPrice(order.getTotalPrice())
                        .status(order.getStatus())
                        .build())
                .toList();

        Long nextLastId = dtoList.isEmpty() ? null : dtoList.get(dtoList.size() - 1).orderId();

        return PageResponseDto.ofCursor(dtoList, size, hasNext, nextLastId);
    }

    public OrderResponse createOrder(Long userId, Long storeId, List<OrderRequest.OrderItemRequest> items) {
        Order order = orderFacade.executeOrder(userId, storeId, items);
        
        // Fetch order items to build response
        List<OrderItem> savedItems = orderItemRepository.findAllByOrderId(order.getId());
        
        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .items(savedItems.stream()
                        .map(item -> OrderResponse.OrderItemResponse.builder()
                                .menuId(item.getMenuId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
