/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

/**
 * @author jitta
 */
public interface WebSocketHandler<T> {

    void onConnect(final WebSocket webSocket);

    void onMessage(final WebSocket webSocket, final T message);

    void onError(final WebSocket webSocket, final Throwable e);

    void onDisconnect(final WebSocket webSocket, final CloseStatus status);

}
