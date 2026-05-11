package com.example.kcomproject.domain.menu.controller;

import com.example.kcomproject.domain.menu.dto.response.MenuResponse;
import com.example.kcomproject.domain.menu.entity.MenuCategory;
import com.example.kcomproject.domain.menu.entity.MenuStatus;
import com.example.kcomproject.domain.menu.service.MenuService;
import com.example.kcomproject.global.dto.ApiResponseDto;
import com.example.kcomproject.global.dto.PageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<MenuResponse>>> getMenus(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MenuCategory category,
            @RequestParam(required = false, defaultValue = "AVAILABLE") MenuStatus status,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        log.info("Fetching menus with keyword: {}, category: {}, status: {}", keyword, category, status);
        return ResponseEntity.ok(ApiResponseDto.success(menuService.getMenus(keyword, category, status, lastId, size)));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponseDto<List<MenuResponse>>> getPopularMenus() {
        return ResponseEntity.ok(ApiResponseDto.success(menuService.getPopularMenus()));
    }
}
