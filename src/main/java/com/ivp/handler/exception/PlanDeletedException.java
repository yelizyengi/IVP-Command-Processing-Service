package com.ivp.handler.exception;

import java.util.UUID;

public class PlanDeletedException extends RuntimeException {
    public PlanDeletedException(UUID planId) {
        super("Plan is deleted: " + planId);
    }
}
