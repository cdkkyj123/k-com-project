package com.example.kcomproject.domain.order.controller;

import com.example.kcomproject.domain.order.service.OrderService;
import com.example.kcomproject.domain.order.dto.request.OrderRequest;
import com.example.kcomproject.domain.order.dto.response.OrderResponse;
import com.example.kcomproject.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.example.kcomproject.domain.order.entity.OrderStatus;
import com.example.kcomproject.global.dto.PageResponseDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<OrderResponse>>> getOrders(
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponseDto.success(orderService.getOrders(userId, startDate, endDate, statuses, lastId, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDto<OrderResponse>> createOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(orderService.createOrder(request.userId(), request.storeId(), request.items())));
    }
}
