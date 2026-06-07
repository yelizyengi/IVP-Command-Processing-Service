package com.ivp.repository;

import com.ivp.domain.model.ExecutionStatus;
import com.ivp.domain.model.PlanExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanExecutionRepository extends JpaRepository<PlanExecution, UUID> {

    Optional<PlanExecution> findByPlanIdAndExecutionDate(UUID planId, LocalDate executionDate);

    @Query("SELECT e FROM PlanExecution e LEFT JOIN FETCH e.orders WHERE e.id = :id")
    Optional<PlanExecution> findByIdWithOrders(UUID id);

    List<PlanExecution> findByStatusAndCreatedAtBefore(ExecutionStatus status, Instant before);
}
