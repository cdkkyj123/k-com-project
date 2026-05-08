package com.example.kcomproject.domain.order.repository;

import com.example.kcomproject.domain.order.entity.Order;
import com.example.kcomproject.domain.order.entity.OrderStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.kcomproject.domain.order.entity.QOrder.order;

@RequiredArgsConstructor
public class OrderQueryRepositoryImpl implements OrderQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Order> findOrdersByFilter(Long userId, LocalDateTime startDate, LocalDateTime endDate, List<OrderStatus> statuses, Long lastId, int size) {
        return queryFactory
                .selectFrom(order)
                .where(
                        order.userId.eq(userId),
                        afterStartDate(startDate),
                        beforeEndDate(endDate),
                        inStatuses(statuses),
                        ltLastId(lastId)
                )
                .limit(size + 1)
                .orderBy(order.id.desc())
                .fetch();
    }

    private BooleanExpression ltLastId(Long lastId) {
        return lastId != null ? order.id.lt(lastId) : null;
    }

    private BooleanExpression afterStartDate(LocalDateTime startDate) {
        return startDate != null ? order.createdAt.goe(startDate) : null;
    }

    private BooleanExpression beforeEndDate(LocalDateTime endDate) {
        return endDate != null ? order.createdAt.loe(endDate) : null;
    }

    private BooleanExpression inStatuses(List<OrderStatus> statuses) {
        return (statuses != null && !statuses.isEmpty()) ? order.status.in(statuses) : null;
    }
}
