package com.example.kcomproject.domain.point.controller;

import com.example.kcomproject.domain.point.service.PointService;
import com.example.kcomproject.domain.point.dto.request.PointChargeRequest;
import com.example.kcomproject.domain.point.dto.response.PointChargeResponse;
import com.example.kcomproject.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponseDto<PointChargeResponse>> chargePoint(@RequestBody PointChargeRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(pointService.chargePoint(request.userId(), request.amount())));
    }
}
