/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

import java.nio.ByteBuffer;

/**
 * @author jitta
 */
public interface WebSocket {

    String getSessionId();

    void send(final String message);

    void send(final ByteBuffer message);

    void send(final FrameData message);
}
