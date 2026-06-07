package com.ivp.outbox;

import com.ivp.domain.model.OutboxEntry;
import com.ivp.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;

    public OutboxPoller(OutboxRepository outboxRepository,
                        KafkaTemplate<String, String> kafkaTemplate,
                        @Value("${ivp.outbox.batch-size:50}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${ivp.outbox.poll-interval-ms:100}")
    @Transactional
    public void poll() {
        List<OutboxEntry> entries = outboxRepository.findUnpublishedForUpdate(batchSize);
        for (OutboxEntry entry : entries) {
            try {
                kafkaTemplate.send(entry.getTopic(), entry.getAggregateId(), entry.getPayload()).get();
                entry.markPublished();
                outboxRepository.save(entry);
            } catch (Exception e) {
                log.error("Failed to publish outbox entry {}: {}", entry.getId(), e.getMessage());
                break;
            }
        }
    }
}
