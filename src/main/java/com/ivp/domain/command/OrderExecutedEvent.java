package com.ivp.domain.command;

import java.util.UUID;

public record OrderExecutedEvent(String orderId, UUID planExecutionOrderId) {}
