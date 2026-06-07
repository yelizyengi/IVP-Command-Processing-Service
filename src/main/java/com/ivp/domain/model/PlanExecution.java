package com.ivp.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plan_execution")
public class PlanExecution {

    @Id
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "execution_date", nullable = false)
    private LocalDate executionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Enumerated(EnumType.STRING)
    private ExecutionResult result;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL)
    private List<ExecutionOrder> orders = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected PlanExecution() {}

    public PlanExecution(UUID id, UUID planId, LocalDate executionDate) {
        this.id = id;
        this.planId = planId;
        this.executionDate = executionDate;
        this.status = ExecutionStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addOrder(ExecutionOrder order) {
        orders.add(order);
        order.setExecution(this);
    }

    public boolean allOrdersResolved() {
        return !orders.isEmpty() && orders.stream().allMatch(o -> o.getStatus().isTerminal());
    }

    public ExecutionResult computeResult() {
        boolean allFilled = orders.stream().allMatch(o -> o.getStatus() == OrderStatus.FILLED);
        boolean allRejected = orders.stream().allMatch(o -> o.getStatus() == OrderStatus.REJECTED);
        if (allFilled) return ExecutionResult.FULLY_FILLED;
        if (allRejected) return ExecutionResult.FULLY_REJECTED;
        return ExecutionResult.PARTIALLY_FILLED;
    }

    public void markCompleted(ExecutionResult result) {
        this.status = ExecutionStatus.COMPLETED;
        this.result = result;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPlanId() { return planId; }
    public LocalDate getExecutionDate() { return executionDate; }
    public ExecutionStatus getStatus() { return status; }
    public ExecutionResult getResult() { return result; }
    public List<ExecutionOrder> getOrders() { return orders; }
}
