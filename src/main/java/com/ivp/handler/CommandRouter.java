package com.ivp.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CommandRouter {

    private static final Logger log = LoggerFactory.getLogger(CommandRouter.class);
    private final Map<String, CommandHandler<?>> handlers;
    private final ObjectMapper objectMapper;

    public CommandRouter(List<CommandHandler<?>> handlerList, ObjectMapper objectMapper) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(CommandHandler::commandType, Function.identity()));
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public void route(String type, String commandId, JsonNode payload) {
        CommandHandler handler = handlers.get(type);
        if (handler == null) {
            log.warn("No handler registered for command type: {}", type);
            throw new UnsupportedCommandException(type);
        }
        Object command = objectMapper.convertValue(payload, handler.commandClass());
        handler.handle(commandId, command);
    }

    public static class UnsupportedCommandException extends RuntimeException {
        public UnsupportedCommandException(String type) {
            super("Unsupported command type: " + type);
        }
    }
}
