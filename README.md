# IVP Command Processing Service

Investment Plan command processor in a CQRS/event-driven architecture. Manages the full lifecycle of recurring investment plans — users create plans with a set of instruments and amounts, the system executes them on schedule by placing orders with the Order Domain, and tracks results until completion. Built to handle failures gracefully: duplicate messages, partial order failures, and crashes between processing steps.

## Quick Start

```bash
docker compose up -d
./mvnw clean verify
```

## Core Challenges Solved

### 1. Cross-Domain Correlation

The Order Domain processes our orders and sends back results asynchronously. The problem: when `OrderExecuted` arrives, how do we know which plan and which execution it belongs to?

**Solution:** Every order we create gets a unique ID (`planExecutionOrderId`). We include it in `CreateOrder`, the Order Domain returns it in `OrderExecuted`/`OrderFailed`, and we use it to look up the original execution in our database.

### 2. Idempotency (at-least-once delivery)

Messages can arrive multiple times. Three layers prevent duplicate processing:

| Layer | Mechanism | Protects Against |
|-------|-----------|-----------------|
| Command dedup | `processed_command` table | Same CreatePlan/DeletePlan arriving twice |
| DB constraint | `UNIQUE(plan_id, execution_date)` | Same ExecutePlan for same date |
| Terminal check | If order already FILLED/REJECTED, skip | Duplicate OrderExecuted/OrderFailed events |

### 3. No Event Loss (Transactional Outbox)

State change and event are saved in the same database transaction. A separate poller publishes events to Kafka. If the app crashes before publishing, the event stays in the outbox and gets published on restart.

## Architecture

```
┌────────────┐         ┌────────────┐
│ API Service│         │  Scheduler │
└─────┬──────┘         └──────┬─────┘
      │ CreatePlan             │ ExecutePlan
      │ DeletePlan             │
      ▼                        ▼
┌──────────────────────────────────────┐
│         ivp-commands  [topic]        │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│                                      │
│       IVP COMMAND SERVICE            │
│                                      │
│   Consumer ──▶ Handler ──▶ Postgres  │
│                                │     │
│                          Outbox Poller│
│                                │     │
└────────────────────────────────┼─────┘
                   │             │
          ┌────────┘             └────────┐
          ▼                               ▼
┌──────────────────┐          ┌───────────────────┐
│ order-commands   │          │  ivp-events       │
│    [topic]       │          │    [topic]        │
└────────┬─────────┘          └─────────┬─────────┘
         │                              │
         ▼                              ▼
┌──────────────────┐          ┌───────────────────┐
│  Order Domain    │          │    Read Model     │
│                  │          └───────────────────┘
│  (processes      │
│   orders and     │
│   sends results) │
└────────┬─────────┘
         │ OrderExecuted
         │ OrderFailed
         ▼
┌──────────────────────────────────────┐
│         order-events  [topic]        │
└──────────────────┬───────────────────┘
                   │
                   ▼
         IVP COMMAND SERVICE
         (processes results,
          emits completion events)
```

The service has **no HTTP API**. All communication is through Kafka topics. External systems produce to topics we consume, and we produce to topics they consume.

## Example Flow

```
INPUT                                    OUTPUT
─────                                    ──────

CreatePlan                               PlanCreated
  userId: "user-1"                         planId: "abc"
  name: "Monthly Tech"
  investments: AAPL €100, GOOGL €50
  executionDay: 15

         ─ ─ ─ ─ ─ (later, on the 15th) ─ ─ ─ ─ ─

ExecutePlan                              CreateOrder (to Order Domain)
  planId: "abc"                            instrument: "AAPL", amount: €100
  executionDate: 2026-03-15                instrument: "GOOGL", amount: €50

         ─ ─ ─ ─ ─ (Order Domain responds) ─ ─ ─ ─

OrderExecuted                            PlanOrderFilled
  planExecutionOrderId: "order-1"          instrument: "AAPL"

OrderFailed                              PlanOrderRejected
  planExecutionOrderId: "order-2"          instrument: "GOOGL"
  reason: "insufficient liquidity"         reason: "insufficient liquidity"

                                         PlanExecutionCompleted
         (all orders have a result)        result: PARTIALLY_FILLED
```

## Design Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Event delivery | Transactional Outbox | No event loss on crash |
| Concurrency | Optimistic locking (`@Version`) | Prevents race on execution completion |
| Plan deletion | Soft delete | In-flight executions still complete |
| Errors | Retry 3x then DLQ | Poison pills don't block the queue |
| Storage | PostgreSQL + Flyway | See persistence justification below |
| Extensibility | Strategy pattern (CommandHandler interface) | New command type = new class, no existing code modified |

## Persistence

**Choice:** PostgreSQL with Flyway migrations.

**Why not in-memory:** Idempotency requires durable state. If the app restarts, in-memory maps lose track of which commands were already processed — duplicates would slip through.

**Why not H2:** H2 behaves differently from production databases under concurrent access and constraint enforcement. Since our idempotency relies on `UNIQUE` constraints and `SELECT FOR UPDATE` (outbox poller), using PostgreSQL ensures tests validate real behavior.

**Why PostgreSQL:** Constraint-based idempotency (`UNIQUE(plan_id, execution_date)`), row-level locking for the outbox poller (`FOR UPDATE`), and JSONB support if we need to extend event storage later. Flyway provides version-controlled schema migrations.

## Contract Extensions

Added `planExecutionOrderId` (UUID) to `CreateOrder`, `OrderExecuted`, `OrderFailed` for correlation. See [docs/contract-extensions.md](docs/contract-extensions.md).

## Testing

```bash
./mvnw test -Dtest="com.ivp.unit.**"    # unit (~2s)
./mvnw test -Dtest="com.ivp.e2e.**"     # e2e with Testcontainers (~45s)
./mvnw clean verify                      # everything
```

## Deployment

```bash
kubectl apply -f k8s/
```

## What I'd Add With More Time

- CDC (Debezium) instead of outbox polling
- Schema registry (Avro)
- Distributed tracing (OpenTelemetry)
- Load testing

## Resilience Boundaries

**Handles gracefully:**
- Duplicate messages (any command or event delivered multiple times)
- App crash between DB write and Kafka publish (outbox recovers)
- Concurrent order results for the same execution (optimistic locking, retry on conflict)
- Partial order failures (independent per instrument)
- Poison pill messages (routed to DLQ after 3 retries with 1s backoff)

**Throughput characteristics (per instance, not load-tested):**
- Outbox poller: ~500 events/sec (100ms poll interval, batch size 50)
- Command processing: ~300 commands/sec (bottleneck: DB round-trip ~3ms per handler)
- Kafka consumer: 3 concurrent threads per topic
- Horizontal scaling: add instances → Kafka rebalances partitions, linear throughput increase

**Known limitations:**
- Order Domain never responds → execution stays PENDING (stale detector alerts after 1 hour, no auto-resolve)
- Outbox poller is single-threaded → ceiling ~500 events/sec per instance
- No schema validation on inbound messages → contract break = runtime failure
- No outbox cleanup → table grows ~1M rows/month under sustained load without maintenance job

## AI Tool Usage

Used Claude Code for boilerplate generation (entities, Kafka config, test scaffolding). Architecture decisions, idempotency strategy, correlation design, and edge case analysis were mine.
