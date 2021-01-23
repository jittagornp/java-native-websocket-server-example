/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * @author jitta
 */
class MultipleWebSocketHandler implements WebSocketHandler<FrameData> {

    private List<WebSocketHandler> handlers;

    public List<WebSocketHandler> getHandlers() {
        if (handlers == null) {
            handlers = new LinkedList<>();
        }
        return handlers;
    }

    public void setHandlers(final List<WebSocketHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void onConnect(final WebSocket webSocket) {
        handlers.stream()
                .forEach(handler -> {
                    try {
                        handler.onConnect(webSocket);
                    } catch (final Throwable e) {
                        handleError(handler, webSocket, e);
                    }
                });
    }

    @Override
    public void onMessage(final WebSocket webSocket, final FrameData frameData) {
        handlers.stream()
                .forEach(handler -> handleMessage(handler, webSocket, frameData));
    }

    @Override
    public void onError(final WebSocket webSocket, final Throwable e) {
        handlers.stream()
                .forEach(handler -> handleError(handler, webSocket, e));
    }

    @Override
    public void onDisconnect(final WebSocket webSocket, final CloseStatus status) {
        handlers.stream()
                .forEach(handler -> handleConnectionCloseFrame(handler, webSocket, null));
    }

    private void handleError(final WebSocketHandler handler, final WebSocket webSocket, final Throwable e) {
        try {
            handler.onError(webSocket, e);
        } catch (final Throwable ex) {
            ex.printStackTrace();
        }
    }

    private void handleMessage(final WebSocketHandler handler, final WebSocket webSocket, final FrameData frameData) {

        /**
         *  |Opcode  | Meaning                             | Reference |
         * -+--------+-------------------------------------+-----------|
         *  | 0      | Continuation Frame                  | RFC 6455  |
         * -+--------+-------------------------------------+-----------|
         *  | 1      | Text Frame                          | RFC 6455  |
         * -+--------+-------------------------------------+-----------|
         *  | 2      | Binary Frame                        | RFC 6455  |
         * -+--------+-------------------------------------+-----------|
         *  | 8      | Connection Close Frame              | RFC 6455  |
         * -+--------+-------------------------------------+-----------|
         *  | 9      | Ping Frame                          | RFC 6455  |
         * -+--------+-------------------------------------+-----------|
         *  | 10     | Pong Frame                          | RFC 6455  |
         * -+--------+-------------------------------------+-----------|
         */

        final Opcode opcode = frameData.getOpcode();
        System.out.println("opcode : " + opcode);

        if (opcode == Opcode.CONTINUATION_FRAME) {
            handleContinuationFrame(handler, webSocket, frameData);
        } else if (opcode == Opcode.TEXT_FRAME) {
            handleTextFrame(handler, webSocket, frameData);
        } else if (opcode == Opcode.BINARY_FRAME) {
            handleBinaryFrame(handler, webSocket, frameData);
        } else if (opcode == Opcode.CONNECTION_CLOSE) {
            handleConnectionCloseFrame(handler, webSocket, convertToCloseStatus(frameData.getPayloadData().array()));
        } else if (opcode == Opcode.PING) {
            handlePingFrame(handler, webSocket, frameData);
        } else if (opcode == Opcode.PONG) {
            handlePongFrame(handler, webSocket, frameData);
        } else {
            throw new UnsupportedOperationException("Unknown opcode " + opcode);
        }
    }

    private void handleContinuationFrame(final WebSocketHandler handler, final WebSocket webSocket, final FrameData frameData) {
        try {
            if (handler instanceof TextWebSocketHandler) {
                final String text = new String(frameData.getPayloadData().array(), StandardCharsets.UTF_8);
                handler.onMessage(webSocket, text);
            } else if (handler instanceof BinaryWebSocketHandler) {
                handler.onMessage(webSocket, frameData.getPayloadData());
            } else {
                handler.onMessage(webSocket, frameData);
            }
        } catch (final Throwable e) {
            handleError(handler, webSocket, e);
        }
    }

    private void handleTextFrame(final WebSocketHandler handler, final WebSocket webSocket, final FrameData frameData) {
        try {
            if (handler instanceof TextWebSocketHandler) {
                final String text = new String(frameData.getPayloadData().array(), StandardCharsets.UTF_8);
                handler.onMessage(webSocket, text);
            } else {
                handler.onMessage(webSocket, frameData);
            }
        } catch (final Throwable e) {
            handleError(handler, webSocket, e);
        }
    }

    private void handleBinaryFrame(final WebSocketHandler handler, final WebSocket webSocket, final FrameData frameData) {
        try {
            if (handler instanceof BinaryWebSocketHandler) {
                handler.onMessage(webSocket, frameData.getPayloadData());
            } else {
                handler.onMessage(webSocket, frameData);
            }
        } catch (final Throwable e) {
            handleError(handler, webSocket, e);
        }
    }

    private void handleConnectionCloseFrame(final WebSocketHandler handler, final WebSocket webSocket, final CloseStatus status) {
        try {
            handler.onDisconnect(webSocket, status);
        } catch (final Throwable e) {
            handleError(handler, webSocket, e);
        }
    }

    private CloseStatus convertToCloseStatus(final byte[] byteArray) {
        if (byteArray.length == 0) {
            return CloseStatus.NORMAL;
        }
        final int code = new BigInteger(byteArray).intValue();
        return CloseStatus.fromCode(code);
    }

    private void handlePingFrame(final WebSocketHandler handler, final WebSocket webSocket, final FrameData frameData) {
        //TODO
    }

    private void handlePongFrame(final WebSocketHandler handler, final WebSocket webSocket, final FrameData frameData) {
        //TODO
    }
}
