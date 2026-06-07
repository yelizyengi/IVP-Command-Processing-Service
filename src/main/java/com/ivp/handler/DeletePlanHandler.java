package com.ivp.handler;

import com.ivp.domain.command.DeletePlanCommand;
import com.ivp.domain.event.PlanDeleted;
import com.ivp.domain.model.InvestmentPlan;
import com.ivp.domain.model.ProcessedCommand;
import com.ivp.kafka.producer.EventPublisher;
import com.ivp.repository.InvestmentPlanRepository;
import com.ivp.repository.ProcessedCommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class DeletePlanHandler implements CommandHandler<DeletePlanCommand> {

    private static final Logger log = LoggerFactory.getLogger(DeletePlanHandler.class);

    private final InvestmentPlanRepository planRepository;
    private final ProcessedCommandRepository processedCommandRepository;
    private final EventPublisher eventPublisher;

    public DeletePlanHandler(InvestmentPlanRepository planRepository,
                             ProcessedCommandRepository processedCommandRepository,
                             EventPublisher eventPublisher) {
        this.planRepository = planRepository;
        this.processedCommandRepository = processedCommandRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String commandType() { return "DeletePlan"; }

    @Override
    public Class<DeletePlanCommand> commandClass() { return DeletePlanCommand.class; }

    @Override
    @Transactional
    public void handle(String commandId, DeletePlanCommand command) {
        if (processedCommandRepository.existsById(commandId)) {
            log.info("Duplicate DeletePlan command: {}", commandId);
            return;
        }

        Optional<InvestmentPlan> optPlan = planRepository.findById(command.planId());
        if (optPlan.isEmpty() || !optPlan.get().isActive()) {
            log.warn("Plan {} not found or already deleted", command.planId());
            processedCommandRepository.save(new ProcessedCommand(commandId, "DeletePlan"));
            return;
        }

        InvestmentPlan plan = optPlan.get();
        plan.markDeleted();
        planRepository.save(plan);
        processedCommandRepository.save(new ProcessedCommand(commandId, "DeletePlan"));

        eventPublisher.publishDomainEvent("PlanDeleted",
                new PlanDeleted(plan.getId()),
                plan.getId().toString());

        log.info("Plan deleted: {}", plan.getId());
    }
}
