package com.example.kcomproject.domain.store.controller;

import com.example.kcomproject.domain.store.dto.response.StoreResponse;
import com.example.kcomproject.domain.store.service.StoreService;
import com.example.kcomproject.global.dto.ApiResponseDto;
import com.example.kcomproject.global.dto.PageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<StoreResponse>>> getStores(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponseDto<StoreResponse> response = storeService.getStores(keyword, lastId, size);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }
}
