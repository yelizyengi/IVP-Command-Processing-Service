package com.ivp.domain.event;

import java.util.UUID;

public record PlanOrderFilled(UUID planId, UUID executionId, UUID executionOrderId, String instrument) {}
