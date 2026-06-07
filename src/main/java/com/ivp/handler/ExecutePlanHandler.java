package com.ivp.handler;

import com.ivp.domain.command.CreateOrderCommand;
import com.ivp.domain.command.ExecutePlanCommand;
import com.ivp.domain.model.*;
import com.ivp.handler.exception.PlanDeletedException;
import com.ivp.handler.exception.PlanNotFoundException;
import com.ivp.kafka.producer.EventPublisher;
import com.ivp.repository.InvestmentPlanRepository;
import com.ivp.repository.PlanExecutionRepository;
import com.ivp.repository.ProcessedCommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class ExecutePlanHandler implements CommandHandler<ExecutePlanCommand> {

    private static final Logger log = LoggerFactory.getLogger(ExecutePlanHandler.class);

    private final InvestmentPlanRepository planRepository;
    private final PlanExecutionRepository executionRepository;
    private final ProcessedCommandRepository processedCommandRepository;
    private final EventPublisher eventPublisher;

    public ExecutePlanHandler(InvestmentPlanRepository planRepository,
                              PlanExecutionRepository executionRepository,
                              ProcessedCommandRepository processedCommandRepository,
                              EventPublisher eventPublisher) {
        this.planRepository = planRepository;
        this.executionRepository = executionRepository;
        this.processedCommandRepository = processedCommandRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String commandType() { return "ExecutePlan"; }

    @Override
    public Class<ExecutePlanCommand> commandClass() { return ExecutePlanCommand.class; }

    @Override
    @Transactional
    public void handle(String commandId, ExecutePlanCommand command) {
        if (executionRepository.findByPlanIdAndExecutionDate(command.planId(), command.executionDate()).isPresent()) {
            log.info("Execution already exists for plan {} on {}", command.planId(), command.executionDate());
            return;
        }

        InvestmentPlan plan = planRepository.findById(command.planId())
                .orElseThrow(() -> new PlanNotFoundException(command.planId()));

        if (!plan.isActive()) {
            throw new PlanDeletedException(command.planId());
        }

        PlanExecution execution = new PlanExecution(UUID.randomUUID(), plan.getId(), command.executionDate());

        for (PlanInvestment investment : plan.getInvestments()) {
            execution.addOrder(new ExecutionOrder(UUID.randomUUID(), investment.getInstrument(), investment.getAmount()));
        }

        executionRepository.save(execution);
        processedCommandRepository.save(new ProcessedCommand(commandId, "ExecutePlan"));

        for (ExecutionOrder order : execution.getOrders()) {
            eventPublisher.publishOrderCommand(new CreateOrderCommand(
                    order.getId(),
                    plan.getUserId(),
                    order.getInstrument(),
                    order.getAmount(),
                    "BUY"
            ));
        }

        log.info("Execution created for plan {} on {} with {} orders",
                plan.getId(), command.executionDate(), execution.getOrders().size());
    }
}
