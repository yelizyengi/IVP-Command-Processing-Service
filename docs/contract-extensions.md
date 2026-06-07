# Contract Extensions

## Fields Added to Order Domain Contracts

### CreateOrder (outbound to order-commands)

| Field | Type | Added | Why |
|-------|------|-------|-----|
| `planExecutionOrderId` | UUID | Yes | Our internal correlation ID. Without it, we cannot map `OrderExecuted`/`OrderFailed` events back to the specific execution order in our system. |
| `userId` | String | Yes | Required by Order Domain to validate the user has an account and can place orders. |
| `orderDirection` | String | Already existed | Always "BUY" for investment plan executions. |

### OrderExecuted (inbound from order-events)

| Field | Type | Added | Why |
|-------|------|-------|-----|
| `planExecutionOrderId` | UUID | Yes | Echoed back from our CreateOrder command. This is how we correlate the result to our internal state. |

### OrderFailed (inbound from order-events)

| Field | Type | Added | Why |
|-------|------|-------|-----|
| `planExecutionOrderId` | UUID | Yes | Same correlation purpose as above. |

## How We'd Request These Changes

In a real scenario:
1. Open a cross-team RFC/design doc explaining the correlation requirement
2. Propose the `planExecutionOrderId` field as an opaque passthrough — Order Domain stores it and returns it, but doesn't interpret it
3. Alternative: use Kafka message headers for correlation (avoids schema change but less explicit)
4. Agree on a timeline and implement behind a feature flag

## Alternatives If Order Domain Cannot Add Fields

1. **Kafka headers**: Attach `planExecutionOrderId` as a Kafka header on the CreateOrder message. If Order Domain preserves headers when emitting events, we can correlate without schema changes.
2. **Local mapping table**: Store a mapping of `(userId, instrument, amount, timestamp)` → `executionOrderId`. Match incoming events by these natural keys. Fragile if the same user has two identical orders at the same time.
3. **Request-response pattern**: Use a dedicated reply topic per execution. Order Domain responds on a topic we specify in the command. More complex but avoids shared schema.

Option 1 (headers) is preferred if the Order Domain's Kafka infrastructure preserves headers through its processing pipeline.
