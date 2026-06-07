package com.ivp.domain.command;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreatePlanCommand(
        String commandId,
        UUID planId,
        @NotBlank String userId,
        @NotBlank String name,
        @NotEmpty List<InvestmentItem> investments,
        @Min(1) @Max(31) int executionDay
) {
    public record InvestmentItem(
            @NotBlank String instrument,
            @Positive BigDecimal amount
    ) {}
}
