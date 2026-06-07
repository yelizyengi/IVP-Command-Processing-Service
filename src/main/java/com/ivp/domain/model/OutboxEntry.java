package com.ivp.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_entry")
public class OutboxEntry {

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OutboxEntry() {}

    public OutboxEntry(UUID id, String aggregateId, String eventType, String topic, String payload) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.published = false;
        this.createdAt = Instant.now();
    }

    public void markPublished() { this.published = true; }

    public UUID getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getTopic() { return topic; }
    public String getPayload() { return payload; }
    public boolean isPublished() { return published; }
}
