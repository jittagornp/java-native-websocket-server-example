/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author jitta
 */
public interface WebSocket {

    String getSessionId();

    SocketChannel getChannel();

    void send(final String text);

    void send(final ByteBuffer byteBuffer);

    void send(final ByteBuffer payloadData, final Opcode opcode);
}
