package com.example.kcomproject.domain.order.service;

import com.example.kcomproject.domain.menu.entity.Menu;
import com.example.kcomproject.domain.menu.entity.MenuCategory;
import com.example.kcomproject.domain.menu.entity.MenuStatus;
import com.example.kcomproject.domain.menu.entity.MenuStock;
import com.example.kcomproject.domain.menu.repository.MenuRepository;
import com.example.kcomproject.domain.menu.repository.MenuStockRepository;
import com.example.kcomproject.domain.order.dto.request.OrderRequest;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@ActiveProfiles("test")
public class DeadlockStressTest {

    private static final Logger log = LoggerFactory.getLogger(DeadlockStressTest.class);

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
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @MockBean
    private com.example.kcomproject.domain.point.service.MockPaymentService mockPaymentService;

    private Long storeId;
    private Long menuId1;
    private Long menuId2;
    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        doNothing().when(mockPaymentService).processPayment(anyLong(), anyLong());

        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        }
        menuStockRepository.deleteAll();
        menuRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Store
        Store store = Store.builder()
                .name("Deadlock Test Store")
                .address("Test Address")
                .status(StoreStatus.OPEN)
                .build();
        storeId = storeRepository.save(store).getId();

        // 2. Menus (ensure fixed order by manually setting IDs or sorting later)
        Menu menu1 = Menu.builder()
                .name("Coffee A")
                .price(1000L)
                .category(MenuCategory.COFFEE)
                .status(MenuStatus.AVAILABLE)
                .build();
        menuId1 = menuRepository.save(menu1).getId();

        Menu menu2 = Menu.builder()
                .name("Coffee B")
                .price(1000L)
                .category(MenuCategory.COFFEE)
                .status(MenuStatus.AVAILABLE)
                .build();
        menuId2 = menuRepository.save(menu2).getId();

        menuStockRepository.save(new MenuStock(storeId, menuId1, 1000));
        menuStockRepository.save(new MenuStock(storeId, menuId2, 1000));

        // 3. Users
        String testSuffix = String.valueOf(System.currentTimeMillis());
        for (int i = 0; i < 100; i++) {
            User user = User.builder().pointBalance(0L).build();
            Long userId = userRepository.save(user).getId();
            pointService.chargePoint(userId, 50000L, "charge-" + userId + "-" + testSuffix);
            userIds.add(userId);
        }
    }

    @Test
    @DisplayName("[STRESS-TEST-004] 역순 주문 요청 시에도 데드락 없이 모든 요청이 처리되어야 한다")
    void deadlockPreventionTest() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // Determine min/max ID to simulate reverse order
        Long minId = Math.min(menuId1, menuId2);
        Long maxId = Math.max(menuId1, menuId2);

        for (int i = 0; i < threadCount; i++) {
            Long userId = userIds.get(i);
            final int index = i;
            executorService.submit(() -> {
                try {
                    List<OrderRequest.OrderItemRequest> items;
                    if (index % 2 == 0) {
                        // Forward order: [Min, Max]
                        items = List.of(
                                new OrderRequest.OrderItemRequest(minId, 1),
                                new OrderRequest.OrderItemRequest(maxId, 1)
                        );
                    } else {
                        // Reverse order: [Max, Min] -> Facade should sort this
                        items = List.of(
                                new OrderRequest.OrderItemRequest(maxId, 1),
                                new OrderRequest.OrderItemRequest(minId, 1)
                        );
                    }
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
        
        log.info("Final Success Count: {}", successCount.get());
        log.info("Final Fail Count: {}", failCount.get());

        // If deadlock occurs, latch.await() will hang or timeout (if we added one)
        // Here we just verify that all 100 requests finished.
        assertThat(successCount.get() + failCount.get()).isEqualTo(100);
        assertThat(successCount.get()).isEqualTo(100); // Plenty of stock
    }
}
