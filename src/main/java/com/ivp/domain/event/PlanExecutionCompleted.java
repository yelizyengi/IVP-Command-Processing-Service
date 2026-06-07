package com.ivp.domain.event;

import com.ivp.domain.model.ExecutionResult;
import java.time.LocalDate;
import java.util.UUID;

public record PlanExecutionCompleted(UUID planId, UUID executionId, LocalDate executionDate, ExecutionResult result) {}
