package com.ivp.unit;

import com.ivp.domain.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlanExecutionTest {

    @Test
    void allOrdersResolved_allFilled_returnsTrue() {
        PlanExecution execution = createExecution(2);
        execution.getOrders().get(0).markFilled("ord-1");
        execution.getOrders().get(1).markFilled("ord-2");

        assertTrue(execution.allOrdersResolved());
    }

    @Test
    void allOrdersResolved_onePending_returnsFalse() {
        PlanExecution execution = createExecution(2);
        execution.getOrders().get(0).markFilled("ord-1");

        assertFalse(execution.allOrdersResolved());
    }

    @Test
    void computeResult_allFilled_returnsFullyFilled() {
        PlanExecution execution = createExecution(2);
        execution.getOrders().get(0).markFilled("ord-1");
        execution.getOrders().get(1).markFilled("ord-2");

        assertEquals(ExecutionResult.FULLY_FILLED, execution.computeResult());
    }

    @Test
    void computeResult_allRejected_returnsFullyRejected() {
        PlanExecution execution = createExecution(2);
        execution.getOrders().get(0).markRejected("ord-1", "no liquidity");
        execution.getOrders().get(1).markRejected("ord-2", "no liquidity");

        assertEquals(ExecutionResult.FULLY_REJECTED, execution.computeResult());
    }

    @Test
    void computeResult_mixed_returnsPartiallyFilled() {
        PlanExecution execution = createExecution(2);
        execution.getOrders().get(0).markFilled("ord-1");
        execution.getOrders().get(1).markRejected("ord-2", "no liquidity");

        assertEquals(ExecutionResult.PARTIALLY_FILLED, execution.computeResult());
    }

    private PlanExecution createExecution(int orderCount) {
        PlanExecution execution = new PlanExecution(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now());
        for (int i = 0; i < orderCount; i++) {
            execution.addOrder(new ExecutionOrder(UUID.randomUUID(), "INST-" + i, new BigDecimal("100.00")));
        }
        return execution;
    }
}
