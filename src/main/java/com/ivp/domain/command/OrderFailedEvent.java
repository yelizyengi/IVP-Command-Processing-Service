package com.ivp.domain.command;

import java.util.UUID;

public record OrderFailedEvent(String orderId, UUID planExecutionOrderId, String reason) {}
