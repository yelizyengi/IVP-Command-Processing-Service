package com.ivp.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ivp.config.KafkaTopicConfig;
import com.ivp.domain.command.CreateOrderCommand;
import com.ivp.domain.model.OutboxEntry;
import com.ivp.repository.OutboxRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTopicConfig topicConfig;

    public EventPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper, KafkaTopicConfig topicConfig) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.topicConfig = topicConfig;
    }

    public void publishDomainEvent(String type, Object event, String aggregateId) {
        String payload = serializeEnvelope(type, event);
        outboxRepository.save(new OutboxEntry(UUID.randomUUID(), aggregateId, type, topicConfig.getIvpEvents(), payload));
    }

    public void publishOrderCommand(CreateOrderCommand command) {
        String payload = serializeEnvelope("CreateOrder", command);
        outboxRepository.save(new OutboxEntry(UUID.randomUUID(), command.planExecutionOrderId().toString(),
                "CreateOrder", topicConfig.getOrderCommands(), payload));
    }

    private String serializeEnvelope(String type, Object payload) {
        try {
            ObjectNode envelope = objectMapper.createObjectNode();
            envelope.put("type", type);
            envelope.set("payload", objectMapper.valueToTree(payload));
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
