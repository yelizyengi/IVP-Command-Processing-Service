package com.ivp.repository;

import com.ivp.domain.model.ExecutionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface ExecutionOrderRepository extends JpaRepository<ExecutionOrder, UUID> {

    @Query("SELECT o FROM ExecutionOrder o JOIN FETCH o.execution WHERE o.id = :id")
    Optional<ExecutionOrder> findByIdWithExecution(UUID id);
}
