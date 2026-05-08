package com.example.kcomproject.domain.order.service;

import com.example.kcomproject.domain.menu.entity.Menu;
import com.example.kcomproject.domain.menu.entity.MenuCategory;
import com.example.kcomproject.domain.menu.entity.MenuStatus;
import com.example.kcomproject.domain.menu.entity.MenuStock;
import com.example.kcomproject.domain.menu.repository.MenuRepository;
import com.example.kcomproject.domain.menu.repository.MenuStockRepository;
import com.example.kcomproject.domain.order.dto.request.OrderRequest;
import com.example.kcomproject.domain.order.repository.OrderRepository;
import com.example.kcomproject.domain.point.service.PointService;
import com.example.kcomproject.domain.store.entity.Store;
import com.example.kcomproject.domain.store.entity.StoreStatus;
import com.example.kcomproject.domain.store.repository.StoreRepository;
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
public class OrderStressTest {

    private static final Logger log = LoggerFactory.getLogger(OrderStressTest.class);

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private PointService pointService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuStockRepository menuStockRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.example.kcomproject.domain.point.service.MockPaymentService mockPaymentService;

    private Long storeId;
    private Long menuId;
    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Mock behavior: do nothing (success)
        org.mockito.Mockito.doNothing().when(mockPaymentService).processPayment(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());

        // Clean up
        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        }
        orderRepository.deleteAll();
        menuStockRepository.deleteAll();
        menuRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Store
        Store store = Store.builder()
                .name("Stress Test Store")
                .address("Test Address")
                .status(StoreStatus.OPEN)
                .build();
        storeId = storeRepository.save(store).getId();

        // 2. Create Menu & Stock (10 items)
        Menu menu = Menu.builder()
                .name("Limited Coffee")
                .price(1000L)
                .category(MenuCategory.COFFEE)
                .status(MenuStatus.AVAILABLE)
                .build();
        menuId = menuRepository.save(menu).getId();

        MenuStock stock = new MenuStock(storeId, menuId, 10);
        menuStockRepository.save(stock);

        // 3. Create 100 Users with 10,000 points each
        String testSuffix = String.valueOf(System.currentTimeMillis());
        for (int i = 0; i < 100; i++) {
            User user = User.builder()
                    .pointBalance(0L)
                    .build();
            Long userId = userRepository.save(user).getId();
            pointService.chargePoint(userId, 10000L, "charge-" + userId + "-" + testSuffix);
            userIds.add(userId);
        }
    }

    @Test
    @DisplayName("[STRESS-TEST-001] 100명이 동시에 10개 남은 메뉴를 주문할 때, 정확히 10명만 성공해야 한다")
    void openRunStressTest() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            Long userId = userIds.get(i);
            executorService.submit(() -> {
                try {
                    List<OrderRequest.OrderItemRequest> items = List.of(
                            new OrderRequest.OrderItemRequest(menuId, 1)
                    );
                    orderFacade.executeOrder(userId, storeId, items);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("Order failed: {}", e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();

        log.info("Total Time: {} ms", (endTime - startTime));
        log.info("Success Count: {}", successCount.get());
        log.info("Fail Count: {}", failCount.get());

        // Verification
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(90);

        MenuStock finalStock = menuStockRepository.findByStoreIdAndMenuId(storeId, menuId).orElseThrow();
        assertThat(finalStock.getQuantity()).isEqualTo(0);

        long orderCount = orderRepository.count();
        assertThat(orderCount).isEqualTo(10);
    }
}
