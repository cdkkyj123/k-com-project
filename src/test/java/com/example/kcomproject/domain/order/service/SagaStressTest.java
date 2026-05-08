package com.example.kcomproject.domain.order.service;

import com.example.kcomproject.domain.order.entity.Order;
import com.example.kcomproject.domain.order.entity.OrderStatus;
import com.example.kcomproject.domain.order.repository.OrderRepository;
import com.example.kcomproject.domain.point.entity.ChargeHistory;
import com.example.kcomproject.domain.point.entity.ChargeStatus;
import com.example.kcomproject.domain.point.repository.ChargeHistoryRepository;
import com.example.kcomproject.domain.point.repository.PointHistoryRepository;
import com.example.kcomproject.domain.point.service.PointService;
import com.example.kcomproject.domain.user.entity.User;
import com.example.kcomproject.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class SagaStressTest {

    private static final Logger log = LoggerFactory.getLogger(SagaStressTest.class);

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private PointService pointService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private ChargeHistoryRepository chargeHistoryRepository;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.example.kcomproject.domain.point.service.MockPaymentService mockPaymentService;

    private final List<Long> orderIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();
    private final long ORDER_PRICE = 10000L;

    @BeforeEach
    void setUp() {
        // Mock behavior: do nothing (success)
        org.mockito.Mockito.doNothing().when(mockPaymentService).processPayment(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());

        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        }
        orderRepository.deleteAll();
        pointHistoryRepository.deleteAll();
        chargeHistoryRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create 50 Users and 50 Orders
        String testSuffix = String.valueOf(System.currentTimeMillis());
        for (int i = 0; i < 50; i++) {
            User user = User.builder()
                    .pointBalance(0L)
                    .build();
            Long userId = userRepository.save(user).getId();
            pointService.chargePoint(userId, 20000L, "initial-" + userId + "-" + testSuffix); // Start with 20000
            
            Order order = Order.builder()
                    .userId(userId)
                    .storeId(1L)
                    .totalPrice(ORDER_PRICE)
                    .status(OrderStatus.PENDING)
                    .build();
            Long orderId = orderRepository.save(order).getId();
            
            // Deduct point for the order initially
            pointService.usePoint(userId, ORDER_PRICE);
            
            userIds.add(userId);
            orderIds.add(orderId);
        }
    }

    @Test
    @DisplayName("[STRESS-TEST-002] 50개의 주문에 대해 중복 거절 요청(총 100회) 인입 시, 환불은 정확히 1번씩만 이루어져야 한다")
    void massiveRejectIdempotencyTest() throws InterruptedException {
        int totalRequests = 100; // 50 orders * 2 requests each
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        AtomicInteger processedCount = new AtomicInteger();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            Long orderId = orderIds.get(i % 50); // Each orderId gets 2 requests
            executorService.submit(() -> {
                try {
                    orderFacade.rejectOrder(orderId);
                    processedCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("Reject failed for order {}: {}", orderId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();

        log.info("Total Time: {} ms", (endTime - startTime));

        // Verification
        for (Long userId : userIds) {
            User user = userRepository.findById(userId).orElseThrow();
            // Initial 20000 - Use 1000 + Refund 1000 = 20000
            assertThat(user.getPointBalance()).isEqualTo(20000L);
        }

        // Each order should have only one SUCCESS refund charge history
        for (Long orderId : orderIds) {
            List<ChargeHistory> histories = chargeHistoryRepository.findAllByUserId(
                    orderRepository.findById(orderId).orElseThrow().getUserId()
            );
            long refundCount = histories.stream()
                    .filter(h -> h.getIdempotencyKey() != null && h.getIdempotencyKey().equals("REFUND-" + orderId) && h.getStatus() == ChargeStatus.SUCCESS)
                    .count();
            assertThat(refundCount).isEqualTo(1);
        }
    }
}
