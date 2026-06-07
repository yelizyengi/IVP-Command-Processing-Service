package com.ivp.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "investment_plan")
public class InvestmentPlan {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "execution_day", nullable = false)
    private int executionDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanInvestment> investments = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InvestmentPlan() {}

    public InvestmentPlan(UUID id, String userId, String name, int executionDay) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.executionDay = executionDay;
        this.status = PlanStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markDeleted() {
        this.status = PlanStatus.DELETED;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == PlanStatus.ACTIVE;
    }

    public void addInvestment(PlanInvestment investment) {
        investments.add(investment);
        investment.setPlan(this);
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public int getExecutionDay() { return executionDay; }
    public PlanStatus getStatus() { return status; }
    public List<PlanInvestment> getInvestments() { return investments; }
    public Instant getCreatedAt() { return createdAt; }
}
