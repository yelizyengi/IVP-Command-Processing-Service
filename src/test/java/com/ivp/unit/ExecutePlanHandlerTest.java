package com.ivp.unit;

import com.ivp.domain.command.ExecutePlanCommand;
import com.ivp.domain.model.*;
import com.ivp.handler.ExecutePlanHandler;
import com.ivp.handler.exception.PlanDeletedException;
import com.ivp.handler.exception.PlanNotFoundException;
import com.ivp.kafka.producer.EventPublisher;
import com.ivp.repository.InvestmentPlanRepository;
import com.ivp.repository.PlanExecutionRepository;
import com.ivp.repository.ProcessedCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutePlanHandlerTest {

    @Mock private InvestmentPlanRepository planRepository;
    @Mock private PlanExecutionRepository executionRepository;
    @Mock private ProcessedCommandRepository processedCommandRepository;
    @Mock private EventPublisher eventPublisher;

    private ExecutePlanHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ExecutePlanHandler(planRepository, executionRepository, processedCommandRepository, eventPublisher);
    }

    @Test
    void executePlan_happyPath_createsExecutionAndPublishesOrders() {
        UUID planId = UUID.randomUUID();
        InvestmentPlan plan = new InvestmentPlan(planId, "user-1", "Test Plan", 15);
        plan.addInvestment(new PlanInvestment(UUID.randomUUID(), "AAPL", new BigDecimal("100")));
        plan.addInvestment(new PlanInvestment(UUID.randomUUID(), "GOOGL", new BigDecimal("50")));

        when(executionRepository.findByPlanIdAndExecutionDate(any(), any())).thenReturn(Optional.empty());
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        var command = new ExecutePlanCommand("cmd-1", planId, LocalDate.of(2026, 3, 15));
        handler.handle("cmd-1", command);

        verify(executionRepository).save(any());
        verify(eventPublisher, times(2)).publishOrderCommand(any());
    }

    @Test
    void executePlan_duplicateDate_skips() {
        UUID planId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 3, 15);
        PlanExecution existing = new PlanExecution(UUID.randomUUID(), planId, date);

        when(executionRepository.findByPlanIdAndExecutionDate(planId, date)).thenReturn(Optional.of(existing));

        var command = new ExecutePlanCommand("cmd-1", planId, date);
        handler.handle("cmd-1", command);

        verify(planRepository, never()).findById(any());
        verify(eventPublisher, never()).publishOrderCommand(any());
    }

    @Test
    void executePlan_planNotFound_throws() {
        UUID planId = UUID.randomUUID();
        when(executionRepository.findByPlanIdAndExecutionDate(any(), any())).thenReturn(Optional.empty());
        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        var command = new ExecutePlanCommand("cmd-1", planId, LocalDate.of(2026, 3, 15));

        assertThrows(PlanNotFoundException.class, () -> handler.handle("cmd-1", command));
    }

    @Test
    void executePlan_planDeleted_throws() {
        UUID planId = UUID.randomUUID();
        InvestmentPlan plan = new InvestmentPlan(planId, "user-1", "Test Plan", 15);
        plan.markDeleted();

        when(executionRepository.findByPlanIdAndExecutionDate(any(), any())).thenReturn(Optional.empty());
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        var command = new ExecutePlanCommand("cmd-1", planId, LocalDate.of(2026, 3, 15));

        assertThrows(PlanDeletedException.class, () -> handler.handle("cmd-1", command));
    }
}
