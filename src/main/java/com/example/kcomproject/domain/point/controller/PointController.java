package com.example.kcomproject.domain.point.controller;

import com.example.kcomproject.domain.point.dto.request.PointChargeRequest;
import com.example.kcomproject.domain.point.dto.request.PointUseRequest;
import com.example.kcomproject.domain.point.dto.response.PointHistoryResponse;
import com.example.kcomproject.domain.point.dto.response.PointResponse;
import com.example.kcomproject.domain.point.entity.PointTransactionType;
import com.example.kcomproject.domain.point.service.PointFacade;
import com.example.kcomproject.domain.point.service.PointService;
import com.example.kcomproject.global.dto.ApiResponseDto;
import com.example.kcomproject.global.dto.PageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;
    private final PointFacade pointFacade;

    @GetMapping("/histories")
    public ResponseEntity<ApiResponseDto<PageResponseDto<PointHistoryResponse>>> getPointHistories(
            @RequestParam Long userId,
            @RequestParam(required = false) PointTransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponseDto.success(pointService.getPointHistories(userId, type, startDate, endDate, lastId, size)));
    }

    @PostMapping("/charge")
    public ResponseEntity<ApiResponseDto<PointResponse>> chargePoint(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody PointChargeRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(pointFacade.chargePoint(request.userId(), request.amount(), idempotencyKey)));
    }

    @PostMapping("/use")
    public ResponseEntity<ApiResponseDto<PointResponse>> usePoint(@RequestBody PointUseRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(pointFacade.usePoint(request.userId(), request.amount())));
    }
}
