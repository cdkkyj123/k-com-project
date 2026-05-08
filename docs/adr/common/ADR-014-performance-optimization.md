# ADR-014: Performance Optimization and Refactoring

## Status
Proposed

## Context
During the system audit, several performance bottlenecks were identified:
1. **N+1 and Duplicate Queries**: Order creation was performing DB lookups for Menu and MenuStock within a loop, and re-querying OrderItems for the API response.
2. **Heavy Redis Aggregation**: Popular menu calculation was pulling 7 days of sorted set data into Java memory for aggregation, which could be expensive as the number of menus grows.
3. **Cache Stampede Risk**: The popular menu cache was being evicted and then repopulated, creating a window where multiple requests might hit the DB/Redis simultaneously.

## Decision
We implemented the following optimizations:

1. **Batch Fetching for Orders**:
   - Added `findAllByStoreIdAndMenuIdIn` to `MenuStockRepository`.
   - Refactored `OrderTransactionService` to pre-fetch all required `Menu` and `MenuStock` entities using `IN` clauses before processing the order items.
   - Reduced DB roundtrips from `O(N)` to `O(1)`.

2. **Order Response Optimization**:
   - Modified `OrderTransactionService.executeOrder` to return an `OrderResult` record containing both the `Order` and its `OrderItem`s.
   - Updated `OrderService` to use the already-saved `OrderItem`s from the result, eliminating the redundant `findAllByOrderId` query.

3. **Redis ZUNIONSTORE Offloading**:
   - Refactored `MenuService.calculatePopularMenus` to use Redisson's `union` function.
   - This moves the aggregation logic from the Application Server (Java) to the Redis Server, reducing network bandwidth and Java CPU/Memory usage.

4. **Zero-Downtime Cache Refresh**:
   - Replaced `@CacheEvict` with `@CachePut` in `PopularMenuCacheScheduler`.
   - The scheduler now updates the cache in the background by overwriting it with fresh data, ensuring that the cache is never empty and preventing cache stampedes.

## Consequences
- **Positive**: Significantly reduced API latency for order creation.
- **Positive**: Reduced resource consumption on the application server.
- **Positive**: Improved system stability during high load by avoiding cache misses for popular data.
- **Neutral**: Slightly more complex code in `OrderTransactionService` to handle batch fetching and mapping.
