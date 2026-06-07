package com.ivp.domain.event;

import java.util.UUID;

public record PlanCreated(UUID planId, String userId, String name, int executionDay) {}
