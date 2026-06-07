package com.ivp.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "execution_order")
public class ExecutionOrder {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    private PlanExecution execution;

    @Column(name = "order_id")
    private String orderId;

    @Column(nullable = false)
    private String instrument;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExecutionOrder() {}

    public ExecutionOrder(UUID id, String instrument, BigDecimal amount) {
        this.id = id;
        this.instrument = instrument;
        this.amount = amount;
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    void setExecution(PlanExecution execution) { this.execution = execution; }

    public void markFilled(String orderId) {
        this.status = OrderStatus.FILLED;
        this.orderId = orderId;
        this.updatedAt = Instant.now();
    }

    public void markRejected(String orderId, String reason) {
        this.status = OrderStatus.REJECTED;
        this.orderId = orderId;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public PlanExecution getExecution() { return execution; }
    public String getOrderId() { return orderId; }
    public String getInstrument() { return instrument; }
    public BigDecimal getAmount() { return amount; }
    public OrderStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
}
