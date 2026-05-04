package com.example.kcomproject.domain.store.controller;

import com.example.kcomproject.domain.store.entity.Store;
import com.example.kcomproject.domain.store.service.StoreService;
import com.example.kcomproject.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    public ApiResponseDto<List<Store>> getStores() {
        List<Store> stores = storeService.findAllStores();
        return ApiResponseDto.success(stores);
    }
}
