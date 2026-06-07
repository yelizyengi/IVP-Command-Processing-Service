package com.ivp.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivp.config.KafkaTopicConfig;
import com.ivp.domain.command.CommandEnvelope;
import com.ivp.handler.CommandRouter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class IvpCommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(IvpCommandConsumer.class);

    private final ObjectMapper objectMapper;
    private final CommandRouter commandRouter;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicConfig topicConfig;

    public IvpCommandConsumer(ObjectMapper objectMapper, CommandRouter commandRouter,
                              KafkaTemplate<String, String> kafkaTemplate, KafkaTopicConfig topicConfig) {
        this.objectMapper = objectMapper;
        this.commandRouter = commandRouter;
        this.kafkaTemplate = kafkaTemplate;
        this.topicConfig = topicConfig;
    }

    @KafkaListener(topics = "${ivp.kafka.topics.ivp-commands}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            CommandEnvelope envelope = objectMapper.readValue(record.value(), CommandEnvelope.class);
            commandRouter.route(envelope.type(), envelope.commandId(), envelope.payload());
            ack.acknowledge();
        } catch (CommandRouter.UnsupportedCommandException e) {
            log.warn("Unsupported command, sending to DLQ: {}", e.getMessage());
            sendToDlq(record);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process command at offset {}: {}", record.offset(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void sendToDlq(ConsumerRecord<String, String> record) {
        kafkaTemplate.send(topicConfig.getIvpCommandsDlq(), record.key(), record.value());
    }
}
