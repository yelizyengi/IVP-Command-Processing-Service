package com.ivp.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "plan_investment")
public class PlanInvestment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private InvestmentPlan plan;

    @Column(nullable = false)
    private String instrument;

    @Column(nullable = false)
    private BigDecimal amount;

    protected PlanInvestment() {}

    public PlanInvestment(UUID id, String instrument, BigDecimal amount) {
        this.id = id;
        this.instrument = instrument;
        this.amount = amount;
    }

    void setPlan(InvestmentPlan plan) { this.plan = plan; }

    public UUID getId() { return id; }
    public String getInstrument() { return instrument; }
    public BigDecimal getAmount() { return amount; }
}
