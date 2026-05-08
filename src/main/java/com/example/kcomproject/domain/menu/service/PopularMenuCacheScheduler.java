package com.example.kcomproject.domain.menu.service;

import com.example.kcomproject.domain.menu.dto.response.MenuResponse;
import com.example.kcomproject.domain.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.cache.annotation.CachePut;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularMenuCacheScheduler {

    private final MenuService menuService;

    @CachePut(value = "popularMenus")
    @Scheduled(fixedRate = 300000) // 5 minutes
    @SchedulerLock(name = "refreshPopularMenuCache", lockAtMostFor = "4m", lockAtLeastFor = "1m")
    public List<MenuResponse> refreshPopularMenuCache() {
        log.info("Refreshing popular menus cache (Background)...");
        List<MenuResponse> popularMenus = menuService.calculatePopularMenus();
        log.info("Popular menus cache updated in background.");
        return popularMenus;
    }
}
