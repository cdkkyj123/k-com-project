package com.example.kcomproject.domain.order.repository;

import com.example.kcomproject.domain.order.entity.Order;
import com.example.kcomproject.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderQueryRepository {
    List<Order> findOrdersByFilter(Long userId, LocalDateTime startDate, LocalDateTime endDate, List<OrderStatus> statuses, Long lastId, int size);
}
