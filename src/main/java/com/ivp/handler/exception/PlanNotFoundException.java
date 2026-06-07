package com.ivp.handler.exception;

import java.util.UUID;

public class PlanNotFoundException extends RuntimeException {
    public PlanNotFoundException(UUID planId) {
        super("Plan not found: " + planId);
    }
}
