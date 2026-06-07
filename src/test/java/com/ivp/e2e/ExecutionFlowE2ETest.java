package com.ivp.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.ivp.integration.IntegrationTestBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class ExecutionFlowE2ETest extends IntegrationTestBase {

    @Value("${ivp.kafka.topics.ivp-commands}")
    private String ivpCommandsTopic;

    @Value("${ivp.kafka.topics.ivp-events}")
    private String ivpEventsTopic;

    @Value("${ivp.kafka.topics.order-commands}")
    private String orderCommandsTopic;

    @Value("${ivp.kafka.topics.order-events}")
    private String orderEventsTopic;

    @Test
    void fullFlow_createExecuteAndReceiveResults_producesCorrectEvents() throws Exception {
        UUID planId = UUID.randomUUID();
        String commandId = UUID.randomUUID().toString();

        Map<String, Object> createPayload = Map.of(
                "planId", planId.toString(),
                "userId", "user-123",
                "name", "Monthly Tech",
                "investments", List.of(
                        Map.of("instrument", "AAPL", "amount", 100.00),
                        Map.of("instrument", "GOOGL", "amount", 50.00)
                ),
                "executionDay", 15
        );

        sendCommand(ivpCommandsTopic, "CreatePlan", commandId, createPayload);

        Consumer<String, String> eventsConsumer = createTestConsumer(ivpEventsTopic);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, String> records = pollRecords(eventsConsumer, Duration.ofMillis(500));
            assertTrue(records.count() > 0);
        });

        String executeCommandId = UUID.randomUUID().toString();
        Map<String, Object> executePayload = Map.of(
                "planId", planId.toString(),
                "executionDate", "2026-03-15"
        );

        sendCommand(ivpCommandsTopic, "ExecutePlan", executeCommandId, executePayload);

        Consumer<String, String> orderConsumer = createTestConsumer(orderCommandsTopic);

        List<ConsumerRecord<String, String>> orderCommands = new ArrayList<>();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            ConsumerRecords<String, String> records = pollRecords(orderConsumer, Duration.ofMillis(500));
            records.forEach(orderCommands::add);
            assertEquals(2, orderCommands.size());
        });

        List<UUID> executionOrderIds = new ArrayList<>();
        for (ConsumerRecord<String, String> record : orderCommands) {
            JsonNode node = objectMapper.readTree(record.value());
            executionOrderIds.add(UUID.fromString(node.get("payload").get("planExecutionOrderId").asText()));
        }

        Map<String, Object> orderExecuted = Map.of(
                "orderId", "ext-order-1",
                "planExecutionOrderId", executionOrderIds.get(0).toString()
        );
        sendOrderEvent(orderEventsTopic, "OrderExecuted", orderExecuted);

        Map<String, Object> orderFailed = Map.of(
                "orderId", "ext-order-2",
                "planExecutionOrderId", executionOrderIds.get(1).toString(),
                "reason", "insufficient liquidity"
        );
        sendOrderEvent(orderEventsTopic, "OrderFailed", orderFailed);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            ConsumerRecords<String, String> records = pollRecords(eventsConsumer, Duration.ofMillis(500));
            List<String> eventTypes = new ArrayList<>();
            records.forEach(r -> {
                try {
                    JsonNode node = objectMapper.readTree(r.value());
                    eventTypes.add(node.get("type").asText());
                } catch (Exception ignored) {}
            });
            assertTrue(eventTypes.contains("PlanExecutionCompleted"));
        });

        eventsConsumer.close();
        orderConsumer.close();
    }
}
