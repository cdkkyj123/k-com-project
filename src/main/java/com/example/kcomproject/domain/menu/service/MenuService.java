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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final RedissonClient redissonClient;

    private static final String POPULAR_MENUS_KEY = "popular_menus:realtime";

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

    @Transactional(readOnly = true)
    public List<MenuResponse> getPopularMenus() {
        RScoredSortedSet<Long> popularMenuIds = redissonClient.getScoredSortedSet(POPULAR_MENUS_KEY);
        Collection<Long> top3Ids = popularMenuIds.valueRangeReversed(0, 2);

        if (top3Ids.isEmpty()) {
            return List.of();
        }

        return menuRepository.findAllById(top3Ids).stream()
                .map(MenuResponse::from)
                .collect(Collectors.toList());
    }
}
