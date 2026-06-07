package com.ivp.handler;

import com.ivp.domain.command.CreatePlanCommand;
import com.ivp.domain.event.PlanCreated;
import com.ivp.domain.model.InvestmentPlan;
import com.ivp.domain.model.PlanInvestment;
import com.ivp.domain.model.ProcessedCommand;
import com.ivp.kafka.producer.EventPublisher;
import com.ivp.repository.InvestmentPlanRepository;
import com.ivp.repository.ProcessedCommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class CreatePlanHandler implements CommandHandler<CreatePlanCommand> {

    private static final Logger log = LoggerFactory.getLogger(CreatePlanHandler.class);

    private final InvestmentPlanRepository planRepository;
    private final ProcessedCommandRepository processedCommandRepository;
    private final EventPublisher eventPublisher;

    public CreatePlanHandler(InvestmentPlanRepository planRepository,
                             ProcessedCommandRepository processedCommandRepository,
                             EventPublisher eventPublisher) {
        this.planRepository = planRepository;
        this.processedCommandRepository = processedCommandRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String commandType() { return "CreatePlan"; }

    @Override
    public Class<CreatePlanCommand> commandClass() { return CreatePlanCommand.class; }

    @Override
    @Transactional
    public void handle(String commandId, CreatePlanCommand command) {
        if (processedCommandRepository.existsById(commandId)) {
            log.info("Duplicate CreatePlan command: {}", commandId);
            return;
        }

        UUID planId = command.planId() != null ? command.planId() : UUID.randomUUID();

        InvestmentPlan plan = new InvestmentPlan(planId, command.userId(), command.name(), command.executionDay());

        for (CreatePlanCommand.InvestmentItem item : command.investments()) {
            plan.addInvestment(new PlanInvestment(UUID.randomUUID(), item.instrument(), item.amount()));
        }

        planRepository.save(plan);
        processedCommandRepository.save(new ProcessedCommand(commandId, "CreatePlan"));

        eventPublisher.publishDomainEvent("PlanCreated",
                new PlanCreated(plan.getId(), plan.getUserId(), plan.getName(), plan.getExecutionDay()),
                plan.getId().toString());

        log.info("Plan created: {} for user {}", planId, command.userId());
    }
}
