package com.ivp.domain.event;

import java.util.UUID;

public record PlanOrderRejected(UUID planId, UUID executionId, UUID executionOrderId, String instrument, String reason) {}
