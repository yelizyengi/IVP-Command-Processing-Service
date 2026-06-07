package com.ivp.domain.command;

import com.fasterxml.jackson.databind.JsonNode;

public record CommandEnvelope(String type, String commandId, JsonNode payload) {}
