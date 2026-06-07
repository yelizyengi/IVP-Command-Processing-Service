package com.ivp.unit;

import com.ivp.domain.command.CreatePlanCommand;
import com.ivp.domain.command.DeletePlanCommand;
import com.ivp.domain.model.InvestmentPlan;
import com.ivp.domain.model.ProcessedCommand;
import com.ivp.handler.CreatePlanHandler;
import com.ivp.handler.DeletePlanHandler;
import com.ivp.kafka.producer.EventPublisher;
import com.ivp.repository.InvestmentPlanRepository;
import com.ivp.repository.ProcessedCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePlanHandlerTest {

    @Mock private InvestmentPlanRepository planRepository;
    @Mock private ProcessedCommandRepository processedCommandRepository;
    @Mock private EventPublisher eventPublisher;

    private CreatePlanHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreatePlanHandler(planRepository, processedCommandRepository, eventPublisher);
    }

    @Test
    void createPlan_happyPath_savesPlanAndPublishesEvent() {
        when(processedCommandRepository.existsById(any())).thenReturn(false);

        var command = new CreatePlanCommand(null, UUID.randomUUID(), "user-1", "Tech Portfolio",
                List.of(new CreatePlanCommand.InvestmentItem("AAPL", new BigDecimal("100.00"))), 15);

        handler.handle("cmd-1", command);

        verify(planRepository).save(any());
        verify(processedCommandRepository).save(any());
        verify(eventPublisher).publishDomainEvent(eq("PlanCreated"), any(), any());
    }

    @Test
    void createPlan_duplicateCommand_skips() {
        when(processedCommandRepository.existsById("cmd-1")).thenReturn(true);

        var command = new CreatePlanCommand(null, UUID.randomUUID(), "user-1", "Tech Portfolio",
                List.of(new CreatePlanCommand.InvestmentItem("AAPL", new BigDecimal("100.00"))), 15);

        handler.handle("cmd-1", command);

        verify(planRepository, never()).save(any());
        verify(eventPublisher, never()).publishDomainEvent(any(), any(), any());
    }

    @Test
    void createPlan_multipleInvestments_allPersisted() {
        when(processedCommandRepository.existsById(any())).thenReturn(false);

        var command = new CreatePlanCommand(null, UUID.randomUUID(), "user-1", "Diverse Portfolio",
                List.of(
                        new CreatePlanCommand.InvestmentItem("AAPL", new BigDecimal("100.00")),
                        new CreatePlanCommand.InvestmentItem("GOOGL", new BigDecimal("50.00")),
                        new CreatePlanCommand.InvestmentItem("MSFT", new BigDecimal("75.00"))
                ), 15);

        handler.handle("cmd-2", command);

        verify(planRepository).save(argThat(plan -> plan.getInvestments().size() == 3));
    }
}
