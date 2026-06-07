package com.ivp.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivp.config.KafkaTopicConfig;
import com.ivp.domain.command.OrderEventEnvelope;
import com.ivp.domain.command.OrderExecutedEvent;
import com.ivp.domain.command.OrderFailedEvent;
import com.ivp.handler.OrderResultHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderResultHandler orderResultHandler;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicConfig topicConfig;

    public OrderEventConsumer(ObjectMapper objectMapper, OrderResultHandler orderResultHandler,
                              KafkaTemplate<String, String> kafkaTemplate, KafkaTopicConfig topicConfig) {
        this.objectMapper = objectMapper;
        this.orderResultHandler = orderResultHandler;
        this.kafkaTemplate = kafkaTemplate;
        this.topicConfig = topicConfig;
    }

    @KafkaListener(topics = "${ivp.kafka.topics.order-events}", groupId = "${spring.kafka.consumer.group-id}-orders")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            OrderEventEnvelope envelope = objectMapper.readValue(record.value(), OrderEventEnvelope.class);

            switch (envelope.type()) {
                case "OrderExecuted" -> {
                    OrderExecutedEvent event = objectMapper.treeToValue(envelope.payload(), OrderExecutedEvent.class);
                    orderResultHandler.handleOrderExecuted(event);
                }
                case "OrderFailed" -> {
                    OrderFailedEvent event = objectMapper.treeToValue(envelope.payload(), OrderFailedEvent.class);
                    orderResultHandler.handleOrderFailed(event);
                }
                default -> log.warn("Unknown order event type: {}", envelope.type());
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process order event at offset {}: {}", record.offset(), e.getMessage());
            sendToDlq(record);
            ack.acknowledge();
        }
    }

    private void sendToDlq(ConsumerRecord<String, String> record) {
        kafkaTemplate.send(topicConfig.getOrderEventsDlq(), record.key(), record.value());
    }
}
