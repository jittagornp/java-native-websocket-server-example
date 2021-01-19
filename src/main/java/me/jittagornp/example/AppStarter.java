/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example;

import me.jittagornp.example.websocket.TextWebSocketHandler;
import me.jittagornp.example.websocket.WebSocket;
import me.jittagornp.example.websocket.WebSocketHandler;
import me.jittagornp.example.websocket.WebSocketServer;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * @author jitta
 */
public class AppStarter {

    public static void main(final String[] args) throws IOException, NoSuchAlgorithmException {

        final WebSocketHandler handler = new TextWebSocketHandler() {
            @Override
            public void onConnect(final WebSocket webSocket) {
                System.out.println("Client connected => " + webSocket);
            }

            @Override
            public void onMessage(final WebSocket webSocket, final String message) {
                System.out.println("Client message => " + message);
                webSocket.send("Server reply : " + message);
            }

            @Override
            public void onError(final WebSocket webSocket, final Throwable e) {
                System.out.println("Client error => " + webSocket);
                System.out.println(e);
            }

            @Override
            public void onDisconnect(final WebSocket webSocket) {
                System.out.println("Client disconnected => " + webSocket);
            }
        };

        WebSocketServer.port(80)
                .addWebSocketHandler(handler)
                .start();
    }
}
