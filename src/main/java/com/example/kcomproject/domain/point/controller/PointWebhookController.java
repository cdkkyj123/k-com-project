package com.example.kcomproject.domain.point.controller;

import com.example.kcomproject.domain.point.dto.request.PaymentWebhookRequest;
import com.example.kcomproject.domain.point.service.PointService;
import com.example.kcomproject.global.dto.ApiResponseDto;
import com.example.kcomproject.global.dto.SuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointWebhookController {

    private final PointService pointService;

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponseDto<String>> receiveWebhook(@RequestBody PaymentWebhookRequest request) {
        pointService.processWebhook(request.getIdempotencyKey(), request.getStatus());
        return ResponseEntity.ok(ApiResponseDto.success("Webhook processed successfully"));
    }
}
