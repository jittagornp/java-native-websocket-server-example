/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author jitta
 */
public class MultipleWebSocketHandler implements WebSocketHandler<FrameData> {

    private final List<WebSocketHandler> handlers;

    public MultipleWebSocketHandler(final List<WebSocketHandler> handlers) {
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
    public void onDisconnect(final WebSocket webSocket) {
        handlers.stream()
                .forEach(handler -> {
                    try {
                        webSocket.getChannel().close();
                        handler.onDisconnect(webSocket);
                    } catch (final IOException e) {
                        handleError(handler, webSocket, e);
                    }
                });
    }

    private void handleError(final WebSocketHandler handler, final WebSocket webSocket, final Throwable e) {
        try {
            handler.onError(webSocket, e);
        } catch (final Throwable ex) {
            ex.printStackTrace();
        }
    }

    private void handleMessage(final WebSocketHandler handler, final WebSocket webSocket, final FrameData frameData) {
        final Opcode opcode = frameData.getOpcode();
        if (opcode == Opcode.CONNECTION_CODE) {
            handleConnectionClose(handler, webSocket, frameData);
        } else if (opcode == Opcode.TEXT_FRAME) {
            handleTextFrame(handler, webSocket, frameData);
        } else if (opcode == Opcode.BINARY_FRAME) {
            handleBinaryFrame(handler, webSocket, frameData);
        } else if (opcode == Opcode.PING) {
            handlePingFrame(handler, webSocket, frameData);
        } else {
            handleOtherFrame(handler, webSocket, frameData);
        }
    }

    private void handleConnectionClose(final WebSocketHandler handler, final WebSocket webSocket, final FrameData frameData) {
        try {
            webSocket.getChannel().close();
            handler.onDisconnect(webSocket);
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

    private void handlePingFrame(final WebSocketHandler handler, final WebSocket webSocket, final FrameData frameData) {
        //TODO
    }

    private void handleOtherFrame(final WebSocketHandler handler, final WebSocket webSocket, final FrameData frameData) {
        try {
            handler.onMessage(webSocket, frameData);
        } catch (final Throwable e) {
            handleError(handler, webSocket, e);
        }
    }
}
