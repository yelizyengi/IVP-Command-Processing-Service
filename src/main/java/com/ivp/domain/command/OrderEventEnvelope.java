package com.ivp.domain.command;

import com.fasterxml.jackson.databind.JsonNode;

public record OrderEventEnvelope(String type, JsonNode payload) {}
