package com.ivp.domain.command;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DeletePlanCommand(String commandId, @NotNull UUID planId) {}
