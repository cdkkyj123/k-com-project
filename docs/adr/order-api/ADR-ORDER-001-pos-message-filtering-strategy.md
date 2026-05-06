# ADR-ORDER-001: 매장별 메시지 라우팅 및 POS 필터링 고도화 전략

## 상태
승인됨 (Accepted)

## 맥락 (Context)
중앙 서버에서 수신된 전국 수천 개 매장의 주문 데이터는 하나의 Kafka 토픽(`coffee-orders`)을 통해 전파됩니다. 각 매장의 POS 시스템은 수많은 주문 중 **자신의 매장에 해당하는 주문만 선별하여 수신**해야 합니다. 또한, 동일 매장 내의 주문은 반드시 발생한 순서대로 처리(In-order delivery)되어야 합니다.

## 결정 (Decision)
1.  **Kafka 메시지 키(Key) 활용**:
    - `storeId`를 Kafka 메시지의 Key로 사용합니다.
    - Kafka의 기본 파티셔너는 동일한 키를 가진 메시지를 동일한 파티션으로 전송하므로, **특정 매장의 주문 순서 보장** 문제를 해결합니다.
2.  **Outbox 엔티티 확장**:
    - `Outbox` 테이블에 `partitionKey` 필드를 추가하여, 메시지 발행 시 사용할 키 정보를 저장합니다.
3.  **POS 필터링 전략 (Message Header vs Payload)**:
    - 메시지 전송 시 Kafka Header에 `storeId`를 포함시킵니다.
    - POS 컨슈머는 전체 페이로드를 역직렬화(Deserialization)하기 전에 **Header 값을 먼저 확인**하여, 자신의 매장 주문이 아닐 경우 즉시 무시(Discard)함으로써 리소스를 절약합니다.
4.  **Outbox 안정성 강화**:
    - `INIT` 상태의 레코드를 단순히 `FAILED`로 바꾸는 대신, `retryCount`를 도입하여 지수 백오프(Exponential Backoff) 전략으로 재시도합니다.

## 논의 과정 (Discussion)
*   **Developer**: "매장마다 토픽을 따로 만들면 안 될까요?"
*   **User**: "프랜차이즈 매장은 수천 개입니다. 토픽 수의 폭발적 증가는 Kafka 클러스터 관리에 치명적입니다."
*   **Developer**: "그렇다면 단일 토픽을 유지하되 `storeId`를 파티션 키로 쓰고, 컨슈머 단에서 필터링하는 것이 현실적입니다."
*   **User**: "필터링 성능을 높이기 위해 페이로드를 다 열어보지 않고 헤더만 보고 버릴 수 있으면 좋겠네요."
*   **Developer**: "네, Kafka Header에 매장 정보를 담아 필터링 효율을 극대화하겠습니다."

## 결과 (Consequences)
- **장점**: 매장별 주문 순서 보장 및 효율적인 메시지 필터링 가능. 인프라 확장성 유지.
- **단점**: Outbox 테이블 스키마 변경 필요. 메시지 발행 시 헤더 추가 로직 필요.
