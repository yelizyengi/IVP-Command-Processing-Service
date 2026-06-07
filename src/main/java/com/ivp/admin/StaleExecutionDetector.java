package com.ivp.admin;

import com.ivp.domain.model.ExecutionStatus;
import com.ivp.domain.model.PlanExecution;
import com.ivp.repository.PlanExecutionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class StaleExecutionDetector {

    private static final Logger log = LoggerFactory.getLogger(StaleExecutionDetector.class);

    private final PlanExecutionRepository executionRepository;
    private final MeterRegistry meterRegistry;

    public StaleExecutionDetector(PlanExecutionRepository executionRepository, MeterRegistry meterRegistry) {
        this.executionRepository = executionRepository;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelay = 60000)
    public void detect() {
        List<PlanExecution> stale = executionRepository.findByStatusAndCreatedAtBefore(
                ExecutionStatus.PENDING, Instant.now().minus(1, ChronoUnit.HOURS));

        if (!stale.isEmpty()) {
            log.warn("Found {} stale executions pending for over 1 hour", stale.size());
            meterRegistry.gauge("ivp.executions.stale", stale.size());
        }
    }
}
