package com.ivp.domain.model;

public enum OrderStatus {
    PENDING, FILLED, REJECTED;

    public boolean isTerminal() {
        return this == FILLED || this == REJECTED;
    }
}
