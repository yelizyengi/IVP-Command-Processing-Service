package com.ivp.domain.command;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ExecutePlanCommand(String commandId, @NotNull UUID planId, @NotNull LocalDate executionDate) {}
