package com.ivp.domain.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderCommand(
        UUID planExecutionOrderId,
        String userId,
        String instrument,
        BigDecimal amount,
        String orderDirection
) {}
