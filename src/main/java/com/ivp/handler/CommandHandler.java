package com.ivp.handler;

public interface CommandHandler<T> {
    String commandType();
    Class<T> commandClass();
    void handle(String commandId, T command);
}
