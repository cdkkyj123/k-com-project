# 설계 문서: 전국 프랜차이즈 스마트 오더 시스템

## 1. 비즈니스 시나리오 및 시스템 정의

본 시스템은 대형 커피 프랜차이즈(예: 스타벅스, 투썸플레이스)의 **모바일 앱 기반 사전 주문 및 매장 픽업 서비스**를 모사한 백엔드 시스템입니다. 수천 개의 매장에서 발생하는 대규모 트래픽을 효율적으로 제어하고, 실시간으로 비즈니스 인사이트를 도출하는 것을 목표로 합니다.

### 1.1 주요 도메인 모델
*   **스마트 오더 (Smart Order)**: 사용자가 앱에서 매장을 선택하고 메뉴를 주문하면, 중앙 서버가 이를 처리한 후 해당 매장의 POS 시스템으로 즉시 전송합니다.
*   **실시간 전국 인기 차트**: 전국 모든 매장에서 발생하는 주문 데이터를 실시간으로 집계하여 사용자에게 "지금 가장 핫한 메뉴" 정보를 제공합니다.
*   **포인트 결제 시스템**: 대규모 동시 접속 환경에서도 안전하게 포인트를 충전하고 주문 금액을 차감합니다.

## 2. 기술적 도전 과제 및 해결 전략

### 2.1 대규모 트래픽 및 동시성 제어 (Concurrency)
*   **문제**: 출근 시간대 등 특정 시간대에 포인트 충전 및 결제 요청이 수만 건 이상 집중됨.
*   **해결**: **Redis Distributed Lock (Redisson)**을 활용하여 사용자 식별자(`userId`) 기반의 분산 락을 적용함으로써 데이터 정합성을 보장하고 DB 부하를 분산함.

### 2.2 실시간 데이터 파이프라인 및 가용성 (Data Consistency)
*   **문제**: 중앙 서버와 매장 POS 간의 직접적인 통신은 네트워크 장애 시 데이터 유실 위험이 크며, 대규모 집계 쿼리는 DB 성능을 저하시킴.
*   **해결**: 
    - **Kafka Cluster**: 주문 정보를 매장별 토픽 파티션으로 분산 발행하여 매장 POS가 안전하게 메시지를 소비(Consume)할 수 있는 구조 구축.
    - **Transactional Outbox Pattern**: DB 트랜잭션과 Kafka 메시지 발행의 원자성을 보장하여 "적어도 한 번(At-Least-Once)"의 데이터 전송을 보장.

### 2.3 대규모 데이터 집계 및 성능 (Scalability)
*   **문제**: 수백만 건의 주문 내역을 DB에서 직접 집계하여 인기 메뉴를 산출하는 것은 실시간성이 떨어지고 비용이 많이 듬.
*   **해결**: Kafka 컨슈머가 실시간 스트림 데이터를 받아 **Redis Sorted Set**에 가중치를 합산하는 방식으로 아키텍처를 구성하여 O(1)의 속도로 랭킹 정보를 반환.

### 2.4 데이터 조회 최적화 (Pagination)
*   **문제**: 매장별 메뉴 목록이나 대량의 히스토리 조회 시 전통적인 Offset 기반 페이징은 데이터 양이 많아질수록 성능이 기하급수적으로 저하됨.
*   **해결**: **Cursor 기반(No-Offset) 페이징**과 **QueryDSL**을 결합하여 데이터 규모와 관계없이 일정한 응답 속도를 유지함.

## 3. 기술 스택 (Tech Stack)
*   **Framework**: Spring Boot 3.x
*   **Language**: Java 17
*   **Build Tool**: Gradle
*   **Database**: MySQL (Source), Redis (Cache/Lock/Ranking)
*   **Messaging**: Kafka Cluster (3 Brokers, KRaft Mode)
*   **ORM**: JPA, QueryDSL

## 4. 데이터 모델 (ERD)

### 4.1 Store (매장)
- `id` (PK): 매장 식별자
- `name`: 매장명
- `address`: 매장 주소
- `status`: 매장 상태 (OPEN, CLOSED)
- `createdAt`: 생성일시
- `updatedAt`: 수정일시

### 4.2 Order (주문)
- `id` (PK): 주문 식별자
- `userId`: 사용자 식별자
- `storeId`: 매장 식별자 (Store ID)
- `menuId`: 메뉴 식별자
- `price`: 주문 당시 가격
- `createdAt`: 주문일시
- `updatedAt`: 수정일시

## 5. API 명세 (API Specification)

### 5.1 Store API
- **GET /api/v1/stores**: 매장 목록 조회

### 5.2 Order API
- **POST /api/v1/orders**: 주문 생성
    - Request Body: `{ "userId": Long, "storeId": Long, "menuId": Long }`
