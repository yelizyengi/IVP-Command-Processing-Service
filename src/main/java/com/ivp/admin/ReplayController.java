package com.ivp.admin;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/admin")
public class ReplayController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String bootstrapServers;

    public ReplayController(KafkaTemplate<String, String> kafkaTemplate,
                            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.kafkaTemplate = kafkaTemplate;
        this.bootstrapServers = bootstrapServers;
    }

    public record ReplayRequest(String sourceTopic, String targetTopic, int maxMessages) {}
    public record ReplayResponse(int messagesReplayed) {}

    @PostMapping("/replay")
    public ReplayResponse replay(@RequestBody ReplayRequest request) {
        int max = request.maxMessages() > 0 ? request.maxMessages() : 100;

        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "ivp-replay-" + System.currentTimeMillis(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );

        AtomicInteger replayed = new AtomicInteger(0);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(request.sourceTopic()));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));

            records.forEach(record -> {
                if (replayed.get() < max) {
                    kafkaTemplate.send(request.targetTopic(), record.key(), record.value());
                    replayed.incrementAndGet();
                }
            });
        }

        return new ReplayResponse(replayed.get());
    }
}
