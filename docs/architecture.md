# Architecture

## System Context

The IVP Command Service operates in a CQRS/event-driven ecosystem:

```
┌────────────┐         ┌───────────────────────────────────────────────────┐
│ API Service│         │          IVP COMMAND SERVICE                       │
│            │────────▶│                                                   │
│  Sends:    │  Kafka  │  ┌──────────┐    ┌──────────┐    ┌──────────┐   │
│  CreatePlan│         │  │ Consumer │───▶│ Handlers │───▶│ Outbox   │   │
│  DeletePlan│         │  └──────────┘    └──────────┘    └────┬─────┘   │
└────────────┘         │                       │               │         │
                       │                       ▼               ▼         │
┌────────────┐         │                 ┌──────────┐    ┌──────────┐   │
│ Scheduler  │────────▶│                 │PostgreSQL│    │  Poller  │   │
│            │  Kafka  │                 └──────────┘    └────┬─────┘   │
│  Sends:    │         │                                      │         │
│  ExecutePlan         │                                      ▼ Kafka   │
└────────────┘         └───────────────────────────────────────────────┘
                                                              │
                       ┌──────────────────────────────────────┼─────────┐
                       │              KAFKA TOPICS             │         │
                       │                                      │         │
                       │  ivp-events ◀────────────────────────┘         │
                       │  order-commands ◀────────────────────┘         │
                       └────────────────────────────────────────────────┘
                                              │
                       ┌──────────────────────▼─────────────────────────┐
                       │           ORDER DOMAIN                          │
                       │  Receives: CreateOrder                         │
                       │  Emits:    OrderExecuted, OrderFailed          │
                       └────────────────────────────────────────────────┘
```

## Data Flow

1. Commands arrive on `ivp-commands` topic (partitioned by planId)
2. Consumer dispatches to appropriate handler via CommandRouter (strategy pattern)
3. Handler validates, updates state in PostgreSQL, writes events to outbox (same transaction)
4. OutboxPoller reads unpublished entries, sends to Kafka, marks as published
5. Order results arrive on `order-events`, correlated via `planExecutionOrderId`
6. OrderResultHandler updates execution state, emits completion events when all orders resolved

## Reliability Guarantees

- **At-least-once delivery**: Manual ACK after processing; retries on failure
- **No event loss**: Transactional outbox ensures events survive crashes
- **Idempotent processing**: Duplicate commands and events handled gracefully
- **Poison pill isolation**: Unprocessable messages route to DLQ, unblocking the queue
- **Horizontal scaling**: Multiple instances consume different partitions in parallel
