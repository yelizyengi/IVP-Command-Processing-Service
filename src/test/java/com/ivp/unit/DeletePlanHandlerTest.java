package com.ivp.unit;

import com.ivp.domain.command.DeletePlanCommand;
import com.ivp.domain.model.InvestmentPlan;
import com.ivp.handler.DeletePlanHandler;
import com.ivp.kafka.producer.EventPublisher;
import com.ivp.repository.InvestmentPlanRepository;
import com.ivp.repository.ProcessedCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeletePlanHandlerTest {

    @Mock private InvestmentPlanRepository planRepository;
    @Mock private ProcessedCommandRepository processedCommandRepository;
    @Mock private EventPublisher eventPublisher;

    private DeletePlanHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DeletePlanHandler(planRepository, processedCommandRepository, eventPublisher);
    }

    @Test
    void deletePlan_happyPath_markDeletedAndPublishesEvent() {
        UUID planId = UUID.randomUUID();
        InvestmentPlan plan = new InvestmentPlan(planId, "user-1", "Test Plan", 15);
        when(processedCommandRepository.existsById(any())).thenReturn(false);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        handler.handle("cmd-1", new DeletePlanCommand("cmd-1", planId));

        verify(planRepository).save(argThat(p -> !p.isActive()));
        verify(eventPublisher).publishDomainEvent(eq("PlanDeleted"), any(), any());
    }

    @Test
    void deletePlan_planNotFound_recordsCommandAndSkips() {
        UUID planId = UUID.randomUUID();
        when(processedCommandRepository.existsById(any())).thenReturn(false);
        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        handler.handle("cmd-2", new DeletePlanCommand("cmd-2", planId));

        verify(processedCommandRepository).save(any());
        verify(eventPublisher, never()).publishDomainEvent(any(), any(), any());
    }

    @Test
    void deletePlan_alreadyDeleted_recordsCommandAndSkips() {
        UUID planId = UUID.randomUUID();
        InvestmentPlan plan = new InvestmentPlan(planId, "user-1", "Test Plan", 15);
        plan.markDeleted();
        when(processedCommandRepository.existsById(any())).thenReturn(false);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        handler.handle("cmd-3", new DeletePlanCommand("cmd-3", planId));

        verify(processedCommandRepository).save(any());
        verify(eventPublisher, never()).publishDomainEvent(any(), any(), any());
    }

    @Test
    void deletePlan_duplicateCommand_skips() {
        when(processedCommandRepository.existsById("cmd-dup")).thenReturn(true);

        handler.handle("cmd-dup", new DeletePlanCommand("cmd-dup", UUID.randomUUID()));

        verify(planRepository, never()).findById(any());
        verify(eventPublisher, never()).publishDomainEvent(any(), any(), any());
    }
}
