package com.example.kcomproject.domain.store.repository;

import com.example.kcomproject.domain.store.entity.Store;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.kcomproject.domain.store.entity.QStore.store;

@RequiredArgsConstructor
public class StoreQueryRepositoryImpl implements StoreQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Store> findStoresByFilter(String keyword, Long lastId, int size) {
        return queryFactory
                .selectFrom(store)
                .where(
                        hasKeyword(keyword),
                        gtLastId(lastId)
                )
                .limit(size + 1)
                .orderBy(store.id.asc())
                .fetch();
    }

    private BooleanExpression gtLastId(Long lastId) {
        return lastId != null ? store.id.gt(lastId) : null;
    }

    private BooleanExpression hasKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? store.name.containsIgnoreCase(keyword) : null;
    }
}
