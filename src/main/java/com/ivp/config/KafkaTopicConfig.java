package com.ivp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ivp.kafka.topics")
public class KafkaTopicConfig {

    private String ivpCommands;
    private String orderEvents;
    private String orderCommands;
    private String ivpEvents;
    private String ivpCommandsDlq;
    private String orderEventsDlq;

    public String getIvpCommands() { return ivpCommands; }
    public void setIvpCommands(String ivpCommands) { this.ivpCommands = ivpCommands; }
    public String getOrderEvents() { return orderEvents; }
    public void setOrderEvents(String orderEvents) { this.orderEvents = orderEvents; }
    public String getOrderCommands() { return orderCommands; }
    public void setOrderCommands(String orderCommands) { this.orderCommands = orderCommands; }
    public String getIvpEvents() { return ivpEvents; }
    public void setIvpEvents(String ivpEvents) { this.ivpEvents = ivpEvents; }
    public String getIvpCommandsDlq() { return ivpCommandsDlq; }
    public void setIvpCommandsDlq(String ivpCommandsDlq) { this.ivpCommandsDlq = ivpCommandsDlq; }
    public String getOrderEventsDlq() { return orderEventsDlq; }
    public void setOrderEventsDlq(String orderEventsDlq) { this.orderEventsDlq = orderEventsDlq; }
}
