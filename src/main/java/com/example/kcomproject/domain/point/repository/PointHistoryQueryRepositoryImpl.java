package com.example.kcomproject.domain.point.repository;

import com.example.kcomproject.domain.point.entity.PointHistory;
import com.example.kcomproject.domain.point.entity.PointTransactionType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.kcomproject.domain.point.entity.QPointHistory.pointHistory;

@RequiredArgsConstructor
public class PointHistoryQueryRepositoryImpl implements PointHistoryQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PointHistory> findHistoriesByFilter(Long userId, PointTransactionType type, LocalDateTime startDate, LocalDateTime endDate, Long lastId, int size) {
        return queryFactory
                .selectFrom(pointHistory)
                .where(
                        pointHistory.userId.eq(userId),
                        hasType(type),
                        afterStartDate(startDate),
                        beforeEndDate(endDate),
                        ltLastId(lastId)
                )
                .limit(size + 1)
                .orderBy(pointHistory.id.desc())
                .fetch();
    }

    private BooleanExpression ltLastId(Long lastId) {
        return lastId != null ? pointHistory.id.lt(lastId) : null;
    }

    private BooleanExpression hasType(PointTransactionType type) {
        return type != null ? pointHistory.type.eq(type) : null;
    }

    private BooleanExpression afterStartDate(LocalDateTime startDate) {
        return startDate != null ? pointHistory.createdAt.goe(startDate) : null;
    }

    private BooleanExpression beforeEndDate(LocalDateTime endDate) {
        return endDate != null ? pointHistory.createdAt.loe(endDate) : null;
    }
}
