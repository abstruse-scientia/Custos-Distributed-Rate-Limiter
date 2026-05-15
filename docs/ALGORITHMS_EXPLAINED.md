# Custos Rate Limiting Algorithms

Custos provides four distinct rate limiting algorithms. Choose base on whether you need burst control, memory efficiency, or strict accuracy.

---

## Token Bucket

### Concept

The Token Bucket algorithm maintains a bucket that holds up to `capacity` tokens. Each request consumes one token. Tokens are continuously replenished at a rate of `rate` tokens per second. If a request arrives when the bucket is empty, the request is rejected. This design permits bursting up to `capacity` requests immediately, followed by a continuous rate of `rate` requests per second.

### Mechanics

```
Bucket:  [████████] ← capacity tokens maximum
         ↓ -1 token per request
         ↓ +rate tokens per second
Result:  Request allowed if tokens > 0
```

When a request arrives:
1. Calculate elapsed time since the last refill
2. Add (elapsed_seconds × rate) tokens to the bucket
3. Cap the total at `capacity` (prevent accumulation beyond max)
4. If tokens ≥ 1, decrement by 1 and allow otherwise reject

### Field Mapping

| Annotation Field | Semantics |
|---|---|
| `capacity` | Maximum tokens the bucket can hold (must be ≥ 1) |
| `rate` | Tokens added per second (must be > 0) |

Example: `capacity=20, rate=0.16` — bucket holds 20 tokens, refills at 0.16 tokens/sec (1 token per 6 seconds).

### Characteristics

- **Burstiness:** Yes. Up to `capacity` requests can proceed instantly.
- **Memory:** Minimal. Stores only current token count and last-refill timestamp.
- **Accuracy:** Exact, within millisecond precision.
- **Jitter:** Possible at burst boundaries smooth after initial burst.

### Recommended Use Cases

- REST APIs that require controlled request bursting
- Scenarios where sustained rate is more important than absolute fairness


---

## Leaky Bucket

### Concept

The Leaky Bucket algorithm treats requests like water pouring into a bucket with a small hole in the bottom. Requests are queued and drain from the queue at a fixed `rate` (requests per second). The bucket has a maximum capacity, if it is full when a new request arrives, that request is rejected. This ensures a strictly uniform output rate, regardless of input patterns.

### Mechanics

```
Input:     Request stream (variable rate)
           ↓
Queue:     [req] [req] [req] ← capacity max depth
           ↓ drains at 'rate' req/sec
Output:    Uniform stream at exactly 'rate' req/sec
```

When a request arrives:
1. Calculate how many requests have leaked since the last arrival
2. Subtract leaked requests from the queue depth
3. If queue_depth < capacity, add incoming request and allow
4. Otherwise, reject the request

### Field Mapping

| Annotation Field | Semantics |
|---|---|
| `capacity` | Maximum queue depth (number of waiting requests) |
| `rate` | Outflow rate in requests per second (must be > 0) |

Example: `capacity=10, rate=2.0` queue can hold 10 requests, drains at exactly 2 requests/sec.

### Characteristics

- **Burstiness:** No. Output is strictly uniform at `rate` requests/sec.
- **Memory:** Minimal. Stores only queue depth and last-leak timestamp.
- **Accuracy:** Exact smoothing no boundary spikes.
- **Jitter:** None. Output is perfectly smooth.

### Recommended Use Cases

- Protecting downstream services with fixed processing capacity
- Scenarios requiring strict rate smoothing with no burstiness
- Rate limiting where output uniformity is critical
- Preventing thundering herd patterns

---

## Sliding Window Log

### Concept

The Sliding Window Log algorithm maintains a log of request timestamps. The sliding window duration is dynamically derived from `capacity / rate` seconds and slides continuously with the clock. When a request arrives, Custos removes all timestamps older than the derived window and checks if the remaining count is below `capacity`. If so, the request is allowed and its timestamp is recorded. This approach is precise because it counts only actual requests within the current window and has no boundary artifacts.

### Mechanics

```
Timeline:  ←======== (capacity / rate) seconds =======>
           [●●●●●] [●●●●] [●]
            past    current future
           
On request: purge timestamps before (now - window_duration)
            if count < capacity: allow and log timestamp
```

When a request arrives at time T:
1. Calculate window duration `W = capacity / rate`
2. Purge all timestamps where timestamp < (T - W)
3. If remaining timestamp count < capacity, allow and record T
4. Otherwise, reject

### Field Mapping

| Annotation Field | Semantics |
|---|---|
| `capacity` | Maximum requests allowed within the derived window |
| `rate` | Allowed request rate per second |

**Note on Window Duration:** As a deliberate design choice, the window duration is not configured directly. Instead, it is implicitly derived from the ratio of capacity to rate (`window_duration_seconds = capacity / rate`). This guarantees mathematical consistency between the burst buffer and the continuous rate.

Example: `capacity=100, rate=25` — sliding window duration is `100 / 25 = 4` seconds. Custos permits a maximum of 100 requests per 4-second window.

### Characteristics

- **Burstiness:** Yes, up to `rate` in a single instant.
- **Memory:** High. Stores a timestamp for every accepted request within the window.
- **Accuracy:** Exact. No approximation or boundary spikes.
- **Jitter:** Possible at window boundaries sharp cutoffs.

### Recommended Use Cases

- Scenarios requiring precise, exact request counting
- SLAs where accuracy to the second is mandated
- Compliance use cases with strict historical auditability
- Systems where timestamp logs are already retained for other reasons

**Trade-off vs. Sliding Window Counter:** Sliding Window Log is more accurate but consumes more memory. For high throughput limits, consider Sliding Window Counter instead.

---

## Sliding Window Counter

### Concept

The Sliding Window Counter algorithm divides time into fixed windows of `capacity` seconds. It maintains counters for the current and previous windows. On each request, Custos calculates a weighted count combining the current window count and a fractional portion of the previous window count. The fraction represents the overlap between the current request time and the previous window. If the weighted count exceeds `rate`, the request is rejected.

### Mechanics

```
Previous Window  │  Current Window
[━━━━━━ 100 ━━━━━━]│[━──── 30 ─────→]
                 │  ↑ request arrives here
                 │  overlap_ratio = (window_end - arrival_time) / window_size
weighted_count = current_count + (previous_count × overlap_ratio)
```

When a request arrives at time T:
1. Determine which window T falls into
2. If window changed, rotate: previous ← current, current ← 0
3. Calculate overlap_ratio = time remaining in previous window / capacity
4. weighted_count = current_window_count + (previous_window_count × overlap_ratio)
5. If weighted_count < rate, increment current_window_count and allow
6. Otherwise, reject

### Field Mapping

| Annotation Field | Semantics |
|---|---|
| `capacity` | Window size in seconds (must be > 0) |
| `rate` | Maximum requests allowed per window |

Example: `capacity=60, rate=100`,  60 second windows, max 100 requests per window.

### Formula

```
weighted_count = current_window_count + 
                 (previous_window_count × overlap_ratio)

overlap_ratio = (window_end_time - request_time) / window_size
```

### Characteristics

- **Burstiness:** Yes, up to `rate` at window boundaries.
- **Memory:** Low. Stores only two counters (current and previous).
- **Accuracy:** Approximate. Weighted interpolation is an estimate, not exact.
- **Jitter:** Possible at window boundaries slightly smoother than pure windowed counting.

### Recommended Use Cases

- High throughput systems where memory efficiency is critical
- Approximate rate limiting is acceptable
- Distributed systems with many rate limit keys
- Scenarios where window based semantics align with business logic (e.g., hourly limits)

**Trade off vs. Sliding Window Log →** Sliding Window Counter is more memory efficient but approximates the count rather than tracking exact timestamps. For strict accuracy requirements, consider Sliding Window Log.

---

## Algorithm Selection Guide

| Algorithm | Burstiness | Memory | Accuracy | Best For                                      |
|---|---|---|---|-----------------------------------------------|
| **Token Bucket** | Yes (up to capacity) | Minimal | Exact | Controlled bursts, APIs with peak tolerance   |
| **Leaky Bucket** | No (uniform only) | Minimal | Exact | Downstream protection, smooth output required |
| **Sliding Window Log** | Yes (up to rate) | High | Exact | Strict accuracy                               |
| **Sliding Window Counter** | Yes (at boundaries) | Low | Approximate | High throughput, memory constrained           |

### Decision Tree

1. **Is downstream service sensitive to bursts?**
   - Yes → Leaky Bucket
   - No → Continue

2. **Is memory constrained?**
   - Yes → Sliding Window Counter
   - No → Continue

3. **Is exact accuracy required?**
   - Yes → Sliding Window Log
   - No → Token Bucket (simpler, same precision in practice)

---

## Configuration Examples

For detailed configuration and usage examples, see [Integration Guides](INTEGRATION_GUIDES.md).

For implementation details of each algorithm, see [Architecture and Flow](ARCHITECTURE_AND_FLOW.md).

---

## State Management

Each algorithm maintains state that must be persisted across requests:

- **Token Bucket:** token count and timestamp of last refill
- **Leaky Bucket:** queue depth and timestamp of last leak
- **Sliding Window Log:** ordered list of request timestamps
- **Sliding Window Counter:** two counters (current and previous window) with window start time

In memory implementations store state in `ConcurrentHashMap`. Redis implementations store state as Redis data structures with automatic TTL based expiration. See [Architecture and Flow](ARCHITECTURE_AND_FLOW.md) for storage details.

