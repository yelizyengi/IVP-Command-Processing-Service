package com.ivp.handler;

import com.ivp.domain.command.OrderExecutedEvent;
import com.ivp.domain.command.OrderFailedEvent;
import com.ivp.domain.event.PlanExecutionCompleted;
import com.ivp.domain.event.PlanOrderFilled;
import com.ivp.domain.event.PlanOrderRejected;
import com.ivp.domain.model.*;
import com.ivp.kafka.producer.EventPublisher;
import com.ivp.repository.ExecutionOrderRepository;
import com.ivp.repository.PlanExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class OrderResultHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderResultHandler.class);

    private final ExecutionOrderRepository orderRepository;
    private final PlanExecutionRepository executionRepository;
    private final EventPublisher eventPublisher;

    public OrderResultHandler(ExecutionOrderRepository orderRepository,
                              PlanExecutionRepository executionRepository,
                              EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.executionRepository = executionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void handleOrderExecuted(OrderExecutedEvent event) {
        processResult(event.planExecutionOrderId(), event.orderId(), OrderStatus.FILLED, null);
    }

    @Transactional
    public void handleOrderFailed(OrderFailedEvent event) {
        processResult(event.planExecutionOrderId(), event.orderId(), OrderStatus.REJECTED, event.reason());
    }

    private void processResult(UUID planExecutionOrderId, String orderId, OrderStatus status, String reason) {
        ExecutionOrder order = orderRepository.findByIdWithExecution(planExecutionOrderId).orElse(null);
        if (order == null) {
            log.warn("Received event for unknown execution order: {}", planExecutionOrderId);
            return;
        }

        if (order.getStatus().isTerminal()) {
            log.info("Order {} already in terminal state, skipping", planExecutionOrderId);
            return;
        }

        if (status == OrderStatus.FILLED) {
            order.markFilled(orderId);
        } else {
            order.markRejected(orderId, reason);
        }
        orderRepository.save(order);

        PlanExecution execution = order.getExecution();
        publishOrderEvent(execution, order, status, reason);
        checkExecutionCompletion(execution);
    }

    private void publishOrderEvent(PlanExecution execution, ExecutionOrder order, OrderStatus status, String reason) {
        String aggregateId = execution.getPlanId().toString();
        if (status == OrderStatus.FILLED) {
            eventPublisher.publishDomainEvent("PlanOrderFilled",
                    new PlanOrderFilled(execution.getPlanId(), execution.getId(), order.getId(), order.getInstrument()),
                    aggregateId);
        } else {
            eventPublisher.publishDomainEvent("PlanOrderRejected",
                    new PlanOrderRejected(execution.getPlanId(), execution.getId(), order.getId(), order.getInstrument(), reason),
                    aggregateId);
        }
    }

    private void checkExecutionCompletion(PlanExecution execution) {
        PlanExecution fullExecution = executionRepository.findByIdWithOrders(execution.getId()).orElseThrow();

        if (fullExecution.allOrdersResolved()) {
            ExecutionResult result = fullExecution.computeResult();
            fullExecution.markCompleted(result);
            executionRepository.save(fullExecution);

            eventPublisher.publishDomainEvent("PlanExecutionCompleted",
                    new PlanExecutionCompleted(fullExecution.getPlanId(), fullExecution.getId(),
                            fullExecution.getExecutionDate(), result),
                    fullExecution.getPlanId().toString());

            log.info("Execution {} completed with result: {}", fullExecution.getId(), result);
        }
    }
}
