/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * @author jitta
 */
public class MultipleWebSocketHandler implements WebSocketHandler<FrameData> {

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

    public void addHandler(final WebSocketHandler handler) {
        getHandlers().add(handler);
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
        final Opcode opcode = frameData.getOpcode();
        System.out.println("opcode : " + opcode);
        if (opcode == Opcode.CONNECTION_CLOSE) {
            handleConnectionCloseFrame(handler, webSocket, frameData);
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

    private void handleConnectionCloseFrame(final WebSocketHandler handler, final WebSocket webSocket, final FrameData frameData) {
        try {
            ((WebSocketImpl) webSocket).getChannel().close();
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
