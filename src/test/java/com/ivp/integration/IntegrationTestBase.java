package com.ivp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    protected KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    protected void sendCommand(String topic, String type, String commandId, Object payload) throws Exception {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("type", type);
        envelope.put("commandId", commandId != null ? commandId : UUID.randomUUID().toString());
        envelope.set("payload", objectMapper.valueToTree(payload));
        kafkaTemplate.send(topic, objectMapper.writeValueAsString(envelope)).get();
    }

    protected void sendOrderEvent(String topic, String type, Object payload) throws Exception {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("type", type);
        envelope.set("payload", objectMapper.valueToTree(payload));
        kafkaTemplate.send(topic, objectMapper.writeValueAsString(envelope)).get();
    }

    protected Consumer<String, String> createTestConsumer(String topic) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "test-" + UUID.randomUUID(), "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(props).createConsumer();
        consumer.subscribe(Collections.singletonList(topic));
        return consumer;
    }

    protected ConsumerRecords<String, String> pollRecords(Consumer<String, String> consumer, Duration timeout) {
        return consumer.poll(timeout);
    }
}
