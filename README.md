# K-Com Coffee Shop System

이 프로젝트는 커피숍 주문 및 포인트 관리 시스템을 다루는 백엔드 시스템입니다. 대규모 트래픽과 다중 인스턴스 환경을 고려하여 설계되었습니다.

## 1. 프로젝트 아키텍처 및 설계 의도

### 1.1 동시성 제어 (Concurrency Control)
- **문제:** 동일 사용자가 여러 단말에서 동시에 포인트를 충전하거나 결제할 때 데이터 부정합 발생 가능성.
- **해결책:** **Redis Distributed Lock (Redisson)** 활용.
- **의도:** DB 비관적 락(Pessimistic Lock)보다 유연하며, 분산 환경에서 특정 키(`userId`)에 대한 원자적 연산을 보장하기 위해 선택하였습니다.

### 1.2 데이터 일관성 (Data Consistency)
- **문제:** 주문 완료 후 외부 시스템(데이터 플랫폼, Kafka)으로의 이벤트 발행 실패 시 데이터 불일치.
- **해결책:** **Transactional Outbox Pattern** 적용.
- **의도:** 주문 트랜잭션 내에 Outbox 이벤트를 함께 저장하여 DB 트랜잭션의 ACID 속성을 이용해 "적어도 한 번(At-Least-Once)"의 이벤트 발행을 보장합니다.

### 1.3 확장성 및 성능 (Scalability & Performance)
- **문제:** 인기 메뉴 조회 등 집계 쿼리가 잦을 경우 DB 부하 증가.
- **해결책:** **Redis Caching + ShedLock**.
- **의도:** 빈번하게 조회되지만 실시간성이 아주 엄격하지 않은 데이터(최근 7일 인기 메뉴)를 Redis에 캐싱하여 응답 속도를 향상시킵니다. 또한, 다중 인스턴스 환경에서 스케줄러가 중복 실행되지 않도록 ShedLock으로 제어합니다.

## 2. 설계 내용

### 2.1 ERD
- **User**: 사용자 정보 및 포인트 잔액 관리.
- **Menu**: 메뉴 정보 (이름, 가격).
- **Order**: 주문 이력 및 메뉴/사용자 참조.
- **PointHistory**: 포인트 충전/사용 상세 내역.
- **Outbox**: Kafka 발행 대기 이벤트 (Aggregate Type, Payload, Status).

### 2.2 API 명세서
1. **GET /api/v1/menus**: 전체 메뉴 목록 조회.
2. **POST /api/v1/points/charge**: 사용자 포인트 충전.
3. **POST /api/v1/orders**: 주문 생성 및 포인트 결제.
4. **GET /api/v1/menus/popular**: 최근 7일간 가장 많이 주문된 상위 3개 메뉴 조회 (캐시 적용).

## 3. 문제 해결 전략 및 분석

### 3.1 동시성 분석
- 사용자의 포인트 잔액은 매우 민감한 데이터이므로, Redisson의 분산 락을 사용하여 순차적 처리를 보장하였습니다.
- 락 획득 타임아웃과 락 유지 시간을 설정하여 데드락을 방지하고 시스템 안정성을 높였습니다.

### 3.2 분산 스케줄링 분석
- `OutboxScheduler`와 `PopularMenuCacheScheduler`는 모든 서버 인스턴스에서 동시에 실행될 필요가 없으며, 중복 실행 시 불필요한 리소스 낭비나 데이터 중복 처리를 야기할 수 있습니다.
- ShedLock을 도입하여 Redis를 락 저장소로 사용함으로써 클러스터 내에서 단 하나의 인스턴스만 해당 작업을 수행하도록 보장하였습니다.

### 3.3 예외 처리 전략
- `GlobalExceptionHandler`를 통해 시스템 전반의 예외를 중앙 집중적으로 관리합니다.
- 비즈니스 로직 예외(`CustomException`)와 시스템 예외를 구분하여 클라이언트에게 일관된 `ErrorResponse` 형식을 반환합니다.
