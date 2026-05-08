# [ADR-013] Redisson Watchdog 도입 및 Consumer 멱등성 보장 전략

## 1. 개요
- **상태**: 승인 (Approved)
- **날짜**: 2026-05-15

## 2. 배경 (Context)
파괴적 테스트(Destructive Audit)를 통해 시스템의 복구력(Resilience)을 검증하던 중 두 가지 치명적인 취약점이 발견되었습니다.
1. **분산 락 타임아웃 (Slow Poison)**: 외부 API 호출이나 시스템 부하로 인해 비즈니스 로직이 설정된 `leaseTime`(5초)을 초과할 경우, 트랜잭션이 완료되지 않았음에도 락이 해제되어 동시성 보호가 깨지는 현상.
2. **Kafka 중복 처리**: 리밸런싱이나 네트워크 이슈로 인해 Kafka Consumer가 동일한 메시지를 중복 수신할 경우, 멱등성 처리가 되어 있지 않아 데이터(인기 메뉴 점수 등)가 중복 합산되는 문제.

## 3. 의사결정 (Decision)

### 3.1 Redisson Watchdog 활성화
- 분산 락 획득 시 `leaseTime`을 `-1`로 설정하여 Redisson의 **Watchdog** 기능을 활성화합니다.
- Watchdog은 트랜잭션이 유지되는 동안 락의 만료 시간을 주기적으로 자동 연장하여, 로직 지연 시에도 동시성 정합성을 완벽히 보장합니다.

### 3.2 Redis SETNX를 통한 Consumer 멱등성 보장
- Kafka Consumer(`PopularMenuConsumer`)에 Redisson의 `RBucket.trySet`(SETNX 명령)을 활용한 멱등성 체크 로직을 도입합니다.
- `idempotency:popular_menu:{orderId}` 키를 사용하여 메시지 처리 여부를 기록하고, 중복 요청 시 로직을 즉시 종료합니다.

## 4. 결정 근거 (Rationale)
1. **정합성 보장**: Watchdog을 통해 "비즈니스 로직 실행 시간 < 락 임대 시간"이라는 불확실한 가정을 제거하고 물리적인 정합성을 확보할 수 있습니다.
2. **복구력 향상**: 장애 복구 과정에서 필연적으로 발생하는 중복 메시지를 안전하게 처리함으로써 데이터 오염을 방지합니다.
3. **최소한의 오버헤드**: Redis의 SETNX 연산은 매우 가볍고 빠르며, Watchdog은 필요한 시점에만 동작하므로 시스템 성능에 미치는 영향이 미미합니다.

## 5. 실행 결과 및 영향
- `OrderFacade`, `PointFacade`의 락 획득 로직 변경.
- `PopularMenuConsumer`의 메시지 처리 프로세스에 멱등성 체크 단계 추가.
- `PointResilienceTest` 및 `KafkaResilienceTest` 통과를 통해 개선 효과 증명.
