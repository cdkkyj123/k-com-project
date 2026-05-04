package com.example.kcomproject.domain.menu.service;

import com.example.kcomproject.domain.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularMenuCacheScheduler {

    private final MenuService menuService;

    @CacheEvict(value = "popularMenus", allEntries = true)
    @Scheduled(fixedRate = 300000) // 5 minutes
    @SchedulerLock(name = "refreshPopularMenuCache", lockAtMostFor = "4m", lockAtLeastFor = "1m")
    public void refreshPopularMenuCache() {
        log.info("Refreshing popular menus cache...");
        menuService.getPopularMenus();
        log.info("Popular menus cache refreshed.");
    }
}
