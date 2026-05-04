package com.example.kcomproject.domain.order.controller;

import com.example.kcomproject.domain.order.service.OrderService;
import com.example.kcomproject.domain.order.dto.request.OrderRequest;
import com.example.kcomproject.domain.order.dto.response.OrderResponse;
import com.example.kcomproject.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<OrderResponse>> createOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(orderService.createOrder(request.userId(), request.storeId(), request.menuId())));
    }
}
