package com.example.kcomproject.domain.order.service;

import com.example.kcomproject.domain.menu.entity.Menu;
import com.example.kcomproject.domain.menu.repository.MenuRepository;
import com.example.kcomproject.domain.store.entity.Store;
import com.example.kcomproject.domain.store.entity.StoreStatus;
import com.example.kcomproject.domain.store.repository.StoreRepository;
import com.example.kcomproject.domain.user.entity.User;
import com.example.kcomproject.domain.user.repository.UserRepository;
import com.example.kcomproject.domain.order.dto.request.OrderRequest;
import com.example.kcomproject.domain.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PointService pointService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private MenuRepository menuRepository;

    private Long userId;
    private Long storeId;
    private Long menuId;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .pointBalance(0L)
                .build();
        userId = userRepository.save(user).getId();

        Store store = Store.builder()
                .name("Test Store")
                .status(StoreStatus.OPEN)
                .build();
        storeId = storeRepository.save(store).getId();

        Menu menu = Menu.builder()
                .name("Coffee")
                .price(1000L)
                .build();
        menuId = menuRepository.save(menu).getId();
    }

    @Test
    @DisplayName("동일 사용자가 동시에 여러 번 주문을 시도할 때, 포인트 잔액만큼만 주문이 생성되어야 한다")
    void concurrentOrderTest() throws InterruptedException {
        // Given: 2500 포인트 충전 (1000원짜리 메뉴 2번 주문 가능)
        pointService.chargePoint(userId, 2500L, "charge-order-test");

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderService.createOrder(userId, storeId, List.of(new OrderRequest.OrderItemRequest(menuId, 1)));
                } catch (Exception e) {
                    // 잔액 부족 등으로 인한 실패 예상
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        User user = userRepository.findById(userId).orElseThrow();
        // 2500 - 1000*2 = 500원 남아야 함
        assertThat(user.getPointBalance()).isEqualTo(500L);
    }
}
