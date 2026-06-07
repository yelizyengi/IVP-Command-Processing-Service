package com.ivp.unit;

import com.ivp.domain.command.OrderExecutedEvent;
import com.ivp.domain.command.OrderFailedEvent;
import com.ivp.domain.model.*;
import com.ivp.handler.OrderResultHandler;
import com.ivp.kafka.producer.EventPublisher;
import com.ivp.repository.ExecutionOrderRepository;
import com.ivp.repository.PlanExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderResultHandlerTest {

    @Mock private ExecutionOrderRepository orderRepository;
    @Mock private PlanExecutionRepository executionRepository;
    @Mock private EventPublisher eventPublisher;

    private OrderResultHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderResultHandler(orderRepository, executionRepository, eventPublisher);
    }

    @Test
    void handleOrderExecuted_happyPath_marksFilledAndPublishesEvent() {
        UUID orderId = UUID.randomUUID();
        PlanExecution execution = createExecutionWithOrder(orderId, "AAPL");

        when(orderRepository.findByIdWithExecution(orderId)).thenReturn(Optional.of(execution.getOrders().get(0)));
        when(executionRepository.findByIdWithOrders(any())).thenReturn(Optional.of(execution));

        handler.handleOrderExecuted(new OrderExecutedEvent("ext-1", orderId));

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.FILLED));
        verify(eventPublisher).publishDomainEvent(eq("PlanOrderFilled"), any(), any());
        verify(eventPublisher).publishDomainEvent(eq("PlanExecutionCompleted"), any(), any());
    }

    @Test
    void handleOrderFailed_marksRejectedAndPublishesEvent() {
        UUID orderId = UUID.randomUUID();
        PlanExecution execution = createExecutionWithOrder(orderId, "AAPL");

        when(orderRepository.findByIdWithExecution(orderId)).thenReturn(Optional.of(execution.getOrders().get(0)));
        when(executionRepository.findByIdWithOrders(any())).thenReturn(Optional.of(execution));

        handler.handleOrderFailed(new OrderFailedEvent("ext-1", orderId, "no liquidity"));

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.REJECTED));
        verify(eventPublisher).publishDomainEvent(eq("PlanOrderRejected"), any(), any());
        verify(eventPublisher).publishDomainEvent(eq("PlanExecutionCompleted"), any(), any());
    }

    @Test
    void handleOrderExecuted_unknownOrder_skips() {
        UUID unknownId = UUID.randomUUID();
        when(orderRepository.findByIdWithExecution(unknownId)).thenReturn(Optional.empty());

        handler.handleOrderExecuted(new OrderExecutedEvent("ext-1", unknownId));

        verify(eventPublisher, never()).publishDomainEvent(any(), any(), any());
    }

    @Test
    void handleOrderExecuted_alreadyTerminal_skipsIdempotently() {
        UUID orderId = UUID.randomUUID();
        PlanExecution execution = createExecutionWithOrder(orderId, "AAPL");
        execution.getOrders().get(0).markFilled("ext-1");

        when(orderRepository.findByIdWithExecution(orderId)).thenReturn(Optional.of(execution.getOrders().get(0)));

        handler.handleOrderExecuted(new OrderExecutedEvent("ext-2", orderId));

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishDomainEvent(any(), any(), any());
    }

    @Test
    void mixedResults_partiallyFilled_completesCorrectly() {
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        PlanExecution execution = new PlanExecution(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now());
        ExecutionOrder order1 = new ExecutionOrder(orderId1, "AAPL", new BigDecimal("100"));
        ExecutionOrder order2 = new ExecutionOrder(orderId2, "GOOGL", new BigDecimal("50"));
        execution.addOrder(order1);
        execution.addOrder(order2);

        // First order fills
        when(orderRepository.findByIdWithExecution(orderId1)).thenReturn(Optional.of(order1));
        when(executionRepository.findByIdWithOrders(execution.getId())).thenReturn(Optional.of(execution));

        handler.handleOrderExecuted(new OrderExecutedEvent("ext-1", orderId1));

        verify(eventPublisher).publishDomainEvent(eq("PlanOrderFilled"), any(), any());
        verify(eventPublisher, never()).publishDomainEvent(eq("PlanExecutionCompleted"), any(), any());

        // Second order fails — now all resolved
        reset(eventPublisher);
        order2 = execution.getOrders().get(1);
        when(orderRepository.findByIdWithExecution(orderId2)).thenReturn(Optional.of(order2));

        handler.handleOrderFailed(new OrderFailedEvent("ext-2", orderId2, "insufficient liquidity"));

        verify(eventPublisher).publishDomainEvent(eq("PlanOrderRejected"), any(), any());
        verify(eventPublisher).publishDomainEvent(eq("PlanExecutionCompleted"), any(), any());
    }

    private PlanExecution createExecutionWithOrder(UUID orderId, String instrument) {
        PlanExecution execution = new PlanExecution(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now());
        execution.addOrder(new ExecutionOrder(orderId, instrument, new BigDecimal("100")));
        return execution;
    }
}
