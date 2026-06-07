package com.ivp.e2e;

import com.ivp.integration.IntegrationTestBase;
import com.ivp.repository.InvestmentPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IdempotencyE2ETest extends IntegrationTestBase {

    @Value("${ivp.kafka.topics.ivp-commands}")
    private String ivpCommandsTopic;

    @Autowired
    private InvestmentPlanRepository planRepository;

    @Test
    void duplicateCreatePlan_onlyOnePlanCreated() throws Exception {
        UUID planId = UUID.randomUUID();
        String commandId = UUID.randomUUID().toString();

        Map<String, Object> payload = Map.of(
                "planId", planId.toString(),
                "userId", "user-1",
                "name", "Duplicate Test",
                "investments", List.of(Map.of("instrument", "AAPL", "amount", 100.00)),
                "executionDay", 1
        );

        sendCommand(ivpCommandsTopic, "CreatePlan", commandId, payload);
        sendCommand(ivpCommandsTopic, "CreatePlan", commandId, payload);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            long count = planRepository.count();
            assertEquals(1, count);
        });
    }
}
