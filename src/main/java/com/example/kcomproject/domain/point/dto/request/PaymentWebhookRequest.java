package com.example.kcomproject.domain.point.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookRequest {
    private String idempotencyKey;
    private String status; // SUCCESS, FAILED 등
}
