package com.example.kcomproject.domain.menu.service;

import com.example.kcomproject.domain.menu.dto.response.MenuResponse;
import com.example.kcomproject.domain.menu.entity.Menu;
import com.example.kcomproject.domain.menu.entity.MenuCategory;
import com.example.kcomproject.domain.menu.entity.MenuStatus;
import com.example.kcomproject.domain.menu.repository.MenuRepository;
import com.example.kcomproject.global.dto.PageResponseDto;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final RedissonClient redissonClient;

    private static final String KEY_PREFIX = "popular_menus:";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Cacheable(value = "menuList", key = "#keyword + '_' + #category + '_' + #status + '_' + #lastId + '_' + #size")
    @Transactional(readOnly = true)
    public PageResponseDto<MenuResponse> getMenus(String keyword, MenuCategory category, MenuStatus status, Long lastId, int size) {
        List<Menu> menus = menuRepository.findMenusByFilter(keyword, category, status, lastId, size);

        boolean hasNext = menus.size() > size;
        List<Menu> content = hasNext ? menus.subList(0, size) : menus;
        Long nextLastId = content.isEmpty() ? null : content.get(content.size() - 1).getId();

        return PageResponseDto.ofCursor(
                content.stream().map(MenuResponse::from).collect(Collectors.toList()),
                size,
                hasNext,
                nextLastId
        );
    }

    @Cacheable(value = "popularMenus")
    @Transactional(readOnly = true)
    public List<MenuResponse> getPopularMenus() {
        LocalDate now = LocalDate.now();
        List<String> keys = IntStream.range(0, 7)
                .mapToObj(i -> KEY_PREFIX + now.minusDays(i).format(FORMATTER))
                .toList();

        // Aggregate scores from the last 7 days in Java
        // Since the number of menus is limited (around 500 as per ADR), this is efficient.
        Map<Long, Double> aggregatedScores = new HashMap<>();

        for (String key : keys) {
            RScoredSortedSet<Long> dailySet = redissonClient.getScoredSortedSet(key);
            dailySet.entryRange(0, -1).forEach(entry ->
                    aggregatedScores.merge(entry.getValue(), entry.getScore(), Double::sum)
            );
        }

        List<Long> top3Ids = aggregatedScores.entrySet().stream()
                .sorted(java.util.Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(3)
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toList());

        if (top3Ids.isEmpty()) {
            return List.of();
        }

        List<Menu> menus = menuRepository.findAllByIdIn(top3Ids);

        return menus.stream()
                .sorted(Comparator.comparingInt(m -> top3Ids.indexOf(m.getId())))
                .map(MenuResponse::from)
                .collect(Collectors.toList());
    }

    @org.springframework.cache.annotation.CacheEvict(value = "menuList", allEntries = true)
    @Transactional
    public void updateMenuStatus(Long menuId, MenuStatus status) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new com.example.kcomproject.global.exception.domain.MenuException(com.example.kcomproject.global.exception.common.ErrorCode.MENU_NOT_FOUND));
        
        menu.updateStatus(status);
        menuRepository.save(menu);
    }
}
