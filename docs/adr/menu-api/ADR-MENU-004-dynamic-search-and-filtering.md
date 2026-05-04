# ADR-MENU-004: 검색 및 카테고리 동적 필터링 도입 (QueryDSL)

## 상태
승인됨 (Accepted)

## 맥락 (Context)
- **이전 상태**: 조건 없이 전체 목록만 조회 가능하거나, JPA Specification을 사용하여 동적 쿼리를 구현함.
- **문제점**: 
    - 메뉴가 다양해질수록 사용자가 특정 메뉴를 찾기 어려워 검색/필터링 기능이 필수적임.
    - JPA Specification은 코드가 다소 가독성이 떨어지며, 복잡한 동적 쿼리 작성 시 유지보수가 어려움.
    - 타입 안정성(Type Safety)을 보장받기 어렵고 컴파일 시점에 오류를 발견하기 힘듦.

## 결정 (Decision)
- **QueryDSL**을 도입하여 동적 쿼리를 구현함.
- `MenuQueryRepository` 인터페이스와 `MenuQueryRepositoryImpl` 구현체를 생성하여 기존 `MenuRepository`와 결합하는 구조를 채택함.
- `JPAQueryFactory`를 사용하여 타입 안정성이 보장된 SQL 스타일의 자바 코드로 필터링 로직을 작성함.

## 근거 (Rationale)
- **타입 안정성**: 컴파일 시점에 Q-Class를 통해 쿼리 오류를 발견할 수 있음.
- **가독성 및 생산성**: 복잡한 조건문과 조인 쿼리를 직관적인 코드로 표현 가능함.
- **표준 구조**: `QueryRepository` 상속 구조를 통해 JPA 표준 리포지토리 기능을 유지하면서 확장성을 확보함.
