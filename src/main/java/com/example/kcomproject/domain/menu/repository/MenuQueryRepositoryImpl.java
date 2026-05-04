package com.example.kcomproject.domain.menu.repository;

import com.example.kcomproject.domain.menu.entity.Menu;
import com.example.kcomproject.domain.menu.entity.MenuCategory;
import com.example.kcomproject.domain.menu.entity.MenuStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.kcomproject.domain.menu.entity.QMenu.menu;

@RequiredArgsConstructor
public class MenuQueryRepositoryImpl implements MenuQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Menu> findMenusByFilter(String keyword, MenuCategory category, MenuStatus status, Long lastId, int size) {
        return queryFactory
                .selectFrom(menu)
                .where(
                        hasKeyword(keyword),
                        hasCategory(category),
                        hasStatus(status),
                        gtLastId(lastId)
                )
                .limit(size + 1) // hasNext 판단을 위해 size + 1 조회
                .orderBy(menu.id.asc()) // ID 기준 오름차순 정렬
                .fetch();
    }

    private BooleanExpression gtLastId(Long lastId) {
        return lastId != null ? menu.id.gt(lastId) : null;
    }

    private BooleanExpression hasKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? menu.name.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression hasCategory(MenuCategory category) {
        return category != null ? menu.category.eq(category) : null;
    }

    private BooleanExpression hasStatus(MenuStatus status) {
        return status != null ? menu.status.eq(status) : null;
    }
}
