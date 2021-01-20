/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

import me.jittagornp.example.util.ByteBufferUtils;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author jitta
 */
class WebSocketImpl implements WebSocket {

    private String sessionId;

    private boolean handshake;

    private final Queue<FrameData> messageQueue;

    public WebSocketImpl() {
        this.messageQueue = new LinkedList<>();
        this.sessionId = UUID.randomUUID().toString();
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    public boolean isHandshake() {
        return handshake;
    }

    public void setHandshake(final boolean handshake) {
        this.handshake = handshake;
    }

    public Queue<FrameData> getMessageQueue() {
        return messageQueue;
    }

    @Override
    public void send(final String message) {
        send(
                FrameData.builder()
                        .fin(true)
                        .rsv1(false)
                        .rsv2(false)
                        .rsv3(false)
                        .opcode(Opcode.TEXT_FRAME)
                        .mask(false)
                        .payloadData(ByteBufferUtils.create(message))
                        .build()
        );
    }

    @Override
    public void send(final ByteBuffer message) {
        send(
                FrameData.builder()
                        .fin(true)
                        .rsv1(false)
                        .rsv2(false)
                        .rsv3(false)
                        .opcode(Opcode.BINARY_FRAME)
                        .mask(false)
                        .payloadData(message)
                        .build()
        );
    }

    @Override
    public void send(final FrameData message) {
        messageQueue.add(message);
    }

    @Override
    public String toString() {
        return "WebSocket{" +
                "sessionId='" + sessionId + '\'' +
                '}';
    }
}
