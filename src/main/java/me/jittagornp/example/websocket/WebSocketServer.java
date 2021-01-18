/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implement follow RFC6455 (The WebSocket Protocol)
 * https://tools.ietf.org/html/rfc6455
 *
 * @author jitta
 */
public class WebSocketServer {

    private static final String RFC6455_CONSTANT = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final int port;

    private final ByteBuffer readByteBuffer;

    private ServerSocketChannel serverSocketChannel;

    private final FrameDataByteBufferConverter converter;

    private List<WebSocketHandler> webSocketHandlers;

    private WebSocketServer(final int port) {
        this.port = port;
        this.webSocketHandlers = new ArrayList<>();
        this.readByteBuffer = ByteBuffer.allocate(1024);
        this.converter = new FrameDataByteBufferConverterImpl();
    }

    public static WebSocketServer port(final int port) {
        return new WebSocketServer(port);
    }

    public WebSocketServer addWebSocketHandler(final WebSocketHandler handler) {
        this.webSocketHandlers.add(handler);
        return this;
    }

    public WebSocketServer setWebSocketHandlers(final List<WebSocketHandler> handlers) {
        this.webSocketHandlers = handlers;
        if (this.webSocketHandlers == null) {
            this.webSocketHandlers = new ArrayList<>();
        }
        return this;
    }

    public void start() throws IOException, NoSuchAlgorithmException {

        System.out.println("WebSocketServer started on port :" + port);

        //1. Define server channel
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));

        //2. Define selector for monitor channels
        final Selector selector = Selector.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        //3. Event loop for monitor channels
        while (true) {
            final int readyChannels = selector.selectNow();
            if (readyChannels > 0) {

                final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                final Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {

                    final SelectionKey key = keyIterator.next();

                    if (key.isAcceptable()) {

                        final SocketChannel clientChannel = serverSocketChannel.accept();
                        final WebSocket webSocket = new WebSocket(clientChannel);

                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ, webSocket);
                        System.out.println("New client connected.");

                    } else if (key.isReadable()) {

                        final WebSocket webSocket = (WebSocket) key.attachment();
                        if (webSocket == null) {
                            key.cancel();
                        }

                        final SocketChannel clientChannel = webSocket.getChannel();
                        if (clientChannel == null) {
                            key.cancel();
                            onDisconnect(webSocket);
                        }

                        while (true) {

                            //Change to Write mode
                            readByteBuffer.clear();

                            //Read data / Write data from channel to byteBuffer
                            int status = 0;
                            try {
                                status = clientChannel.read(readByteBuffer);
                            } catch (final IOException e) {
                                onError(webSocket, e);
                            }

                            if (status <= 0) {
                                break;
                            }

                            //Change to Read mode
                            readByteBuffer.flip();

                            final String secWebSocketKey = getSecWebSocketKey(readByteBuffer);
                            if (secWebSocketKey != null) {
                                doHandShake(secWebSocketKey, webSocket);
                            } else {
                                processFrameData(readByteBuffer, webSocket);
                            }

                        }

                    }
                    keyIterator.remove();
                }
            }
        }
    }

    private void doHandShake(final String secWebSocketKey, final WebSocket webSocket) throws IOException, NoSuchAlgorithmException {
        final String response = buildHandshakeResponse(secWebSocketKey);
        final ByteBuffer byteBuffer = convertToByteBuffer(response);
        webSocket.getChannel().write(byteBuffer);

        System.out.println("===============================");
        System.out.println("Handshake...");
        System.out.println("Request Sec-WebSocket-Key : " + secWebSocketKey);
        System.out.println("-------------------------------");
        System.out.println("Http Response : ");
        System.out.println(response);

        onConnect(webSocket);
    }

    private ByteBuffer convertToByteBuffer(final String text) {
        final byte[] byteArray = text.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer byteBuffer = ByteBuffer.allocate(byteArray.length);
        byteBuffer.put(byteArray);
        byteBuffer.flip();
        return byteBuffer;
    }

    public void stop() throws IOException {
        serverSocketChannel.close();
    }

    private String getSecWebSocketKey(final ByteBuffer byteBuffer) {

        if (!byteBuffer.hasArray()) {
            return null;
        }

        final String text = new String(byteBuffer.array(), StandardCharsets.UTF_8);
        if (text.isEmpty()) {
            return null;
        }

        final boolean isHttpGET = text.startsWith("GET /");
        if (!isHttpGET) {
            return null;
        }

        System.out.println("===============================");
        System.out.println("Http Request");
        System.out.println(text);

        final Pattern pattern = Pattern.compile("Sec-WebSocket-Key: (.*?)\\r\\n");
        final Matcher matcher = pattern.matcher(text);
        matcher.find();
        return matcher.group(1);
    }

    private String buildAcceptKey(final String secWebSocketKey) throws NoSuchAlgorithmException {
        final String concatKey = secWebSocketKey + RFC6455_CONSTANT;
        final byte[] sha1Bytes = MessageDigest.getInstance("SHA-1").digest(concatKey.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sha1Bytes);
    }

    private String buildHandshakeResponse(final String secWebSocketKey) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final String secWebSocketAccept = buildAcceptKey(secWebSocketKey);
        return new StringBuilder()
                .append("HTTP/1.1 101 Switching Protocols\r\n")
                .append("Connection: Upgrade\r\n")
                .append("Upgrade: websocket\r\n")
                .append("Sec-WebSocket-Accept: ")
                .append(secWebSocketAccept)
                .append("\r\n\r\n")
                .toString();
    }

    private void processFrameData(final ByteBuffer byteBuffer, final WebSocket webSocket) {
        final List<ByteBuffer> byteBuffers = Collections.singletonList(byteBuffer);
        List<FrameData> frames = null;
        try {
            frames = converter.convertToFrameData(byteBuffers);
        } catch (final Exception e) {
            frames = Collections.emptyList();
            onError(webSocket, e);
        }

        for (FrameData frameData : frames) {
            onMessage(webSocket, frameData.getOpcode(), frameData.getPayloadData());
        }
    }

    private void onConnect(final WebSocket webSocket) {
        webSocketHandlers.stream()
                .forEach(handler -> {
                    try {
                        handler.onConnect(webSocket);
                    } catch (final Throwable e) {
                        handleError(handler, webSocket, e);
                    }
                });
    }

    private void onMessage(final WebSocket webSocket, final Opcode opcode, final ByteBuffer byteBuffer) {
        webSocketHandlers.stream()
                .forEach(handler -> {

                    if (opcode == Opcode.CONNECTION_CODE) {
                        try {
                            webSocket.getChannel().close();
                            handler.onDisconnect(webSocket);
                        } catch (final Throwable e) {
                            handleError(handler, webSocket, e);
                        }
                    } else if (opcode == Opcode.TEXT_FRAME) {
                        try {
                            if (handler instanceof TextWebSocketHandler) {
                                final String message = new String(byteBuffer.array(), StandardCharsets.UTF_8);
                                handler.onMessage(webSocket, message);
                            } else {
                                handler.onMessage(webSocket, byteBuffer);
                            }
                        } catch (final Throwable e) {
                            handleError(handler, webSocket, e);
                        }
                    } else {
                        try {
                            handler.onMessage(webSocket, byteBuffer);
                        } catch (final Throwable e) {
                            handleError(handler, webSocket, e);
                        }
                    }

                });
    }

    private void onError(final WebSocket webSocket, final Throwable e) {
        webSocketHandlers.stream()
                .forEach(handler -> handleError(handler, webSocket, e));
    }

    private void handleError(final WebSocketHandler handler, final WebSocket webSocket, final Throwable e) {
        try {
            handler.onError(webSocket, e);
        } catch (final Throwable ex) {
            ex.printStackTrace();
        }
    }

    private void onDisconnect(final WebSocket webSocket) {
        webSocketHandlers.stream()
                .forEach(handler -> {
                    try {
                        webSocket.getChannel().close();
                        handler.onDisconnect(webSocket);
                    } catch (final IOException e) {
                        handleError(handler, webSocket, e);
                    }
                });
    }
}
