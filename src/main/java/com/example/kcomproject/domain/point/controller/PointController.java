package com.example.kcomproject.domain.point.controller;

import com.example.kcomproject.domain.point.dto.request.PointUseRequest;
import com.example.kcomproject.domain.point.dto.response.PointResponse;
import com.example.kcomproject.domain.point.service.PointService;
import com.example.kcomproject.domain.point.dto.request.PointChargeRequest;
import com.example.kcomproject.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponseDto<PointResponse>> chargePoint(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody PointChargeRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(pointService.chargePoint(request.userId(), request.amount(), idempotencyKey)));
    }

    @PostMapping("/use")
    public ResponseEntity<ApiResponseDto<PointResponse>> usePoint(@RequestBody PointUseRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(pointService.usePoint(request.userId(), request.amount())));
    }
}
