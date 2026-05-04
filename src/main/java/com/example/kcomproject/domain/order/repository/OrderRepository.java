package com.example.kcomproject.domain.order.repository;

import com.example.kcomproject.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
