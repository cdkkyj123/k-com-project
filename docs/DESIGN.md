# 설계 문서: 전국 프랜차이즈 스마트 오더 시스템

## 1. 비즈니스 시나리오 및 시스템 정의
본 시스템은 스타벅스의 '사이렌 오더'와 같은 **전국 규모 프랜차이즈의 모바일 주문 및 실시간 매장 관제 시스템**을 모델로 합니다. 중앙에서 대규모 주문을 처리하고, Kafka를 통해 각 매장 POS로 주문을 전파하며, 실시간으로 인기 메뉴를 집계합니다.

---

## 2. 상세 설계 내용 (ERD)

### 2.1 User (사용자)
- `id` (PK, Long): 사용자 식별자
- `point_balance` (Long): 현재 보유 포인트 잔액
- `created_at` (LocalDateTime): 가입 일시
- `updated_at` (LocalDateTime): 수정 일시

### 2.2 Store (매장)
- `id` (PK, Long): 매장 식별자
- `name` (String): 매장명 (예: 강남역점)
- `address` (String): 매장 주소
- `status` (Enum): 영업 상태 (`OPEN`, `CLOSED`)
- `created_at`, `updated_at`

### 2.3 Menu (메뉴)
- `id` (PK, Long): 메뉴 식별자
- `name` (String): 메뉴명
- `price` (Long): 가격
- `category` (Enum): 카테고리 (`COFFEE`, `ADE`, `DESSERT` 등)
- `status` (Enum): 판매 상태 (`AVAILABLE`, `OUT_OF_STOCK`, `HIDDEN`)
- `created_at`, `updated_at`

### 2.4 Order (주문)
- `id` (PK, Long): 주문 식별자
- `user_id` (Long): 주문자 ID (ID 기반 참조)
- `store_id` (Long): 주문 매장 ID (ID 기반 참조)
- `menu_id` (Long): 주문 메뉴 ID (ID 기반 참조)
- `price` (Long): 결제 당시 가격
- `created_at`, `updated_at`

### 2.5 PointHistory (포인트 내역)
- `id` (PK, Long): 내역 식별자
- `user_id` (Long): 사용자 ID
- `type` (Enum): 변동 유형 (`CHARGE`, `USE`)
- `amount` (Long): 변동 금액
- `balance_after` (Long): 변동 후 잔액
- `created_at`, `updated_at`

### 2.6 Outbox (이벤트 아웃박스)
- `id` (PK, Long): 이벤트 식별자
- `aggregate_type` (String): 도메인 타입 (예: `ORDER`)
- `aggregate_id` (Long): 관련 엔티티 ID
- `payload` (Text/JSON): 메시지 내용 (storeId 포함)
- `status` (Enum): 전송 상태 (`INIT`, `SENT`, `FAILED`)
- `created_at`, `updated_at`

---

## 3. API 명세서

### 3.1 매장 API
*   **GET /api/v1/stores**
    *   설명: 전체 매장 목록 조회.
    *   응답: `ApiResponseDto<List<StoreResponse>>`

### 3.2 메뉴 API
*   **GET /api/v1/menus**
    *   설명: 필터링 및 커서 기반 메뉴 목록 조회.
    *   파라미터: `keyword`(검색), `category`(분류), `lastId`(커서), `size`(개수)
    *   응답: `ApiResponseDto<PageResponseDto<MenuResponse>>`
*   **GET /api/v1/menus/popular**
    *   설명: 최근 7일간 전국 인기 메뉴 TOP 3 조회 (Redis 실시간 집계 데이터).
    *   응답: `ApiResponseDto<List<MenuResponse>>`

### 3.3 포인트 API
*   **POST /api/v1/points/charge**
    *   설명: 포인트 충전 (분산 락 적용).
    *   요청: `{ "userId": 1, "amount": 10000 }`
    *   응답: `ApiResponseDto<PointChargeResponse>`

### 3.4 주문 API
*   **POST /api/v1/orders**
    *   설명: 주문 생성 및 포인트 결제 (매장 상태 검증 및 Kafka 발행).
    *   요청: `{ "userId": 1, "storeId": 1, "menuId": 1 }`
    *   응답: `ApiResponseDto<OrderResponse>`

---

## 4. 기술적 해결 전략 및 분석

### 4.1 동시성 및 데이터 일관성
- **Redisson 분산 락**: `userId` 기반의 락을 통해 고동시성 환경에서 포인트 잔액 부정합 방지.
- **Transactional Outbox**: 주문 DB 저장과 Kafka 이벤트 발행의 원자성을 보장하여 메시지 유실 차단.

### 4.2 대규모 데이터 처리 아키텍처
- **Kafka 스트리밍 집계**: 전국 매장의 주문 이벤트를 수집하여 Redis Sorted Set에 실시간 점수 누적.
- **슬라이딩 윈도우 (Sliding Window)**: 일자별 Redis 키(`popular_menus:yyyyMMdd`) 분리 및 합산을 통해 정확한 '최근 7일' 통계 제공 및 메모리 효율화(TTL 적용).
- **Cursor 기반 페이징**: 대량 데이터 조회 시 `OFFSET`을 제거하고 인덱스 탐색을 통해 O(1) 성능 유지.
