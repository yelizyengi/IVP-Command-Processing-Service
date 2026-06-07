package com.ivp.repository;

import com.ivp.domain.model.InvestmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InvestmentPlanRepository extends JpaRepository<InvestmentPlan, UUID> {}
