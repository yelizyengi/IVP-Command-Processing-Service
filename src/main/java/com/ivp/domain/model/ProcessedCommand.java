package com.ivp.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "processed_command")
public class ProcessedCommand {

    @Id
    @Column(name = "command_id")
    private String commandId;

    @Column(name = "command_type", nullable = false)
    private String commandType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedCommand() {}

    public ProcessedCommand(String commandId, String commandType) {
        this.commandId = commandId;
        this.commandType = commandType;
        this.processedAt = Instant.now();
    }

    public String getCommandId() { return commandId; }
}
