/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

import me.jittagornp.example.util.ByteBufferUtils;
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

    private static final int READ_BUFFER_SIZE = 100; //100 bytes

    private static final String RFC6455_CONSTANT = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final int port;

    private final FrameDataByteBufferConverter converter;

    private ServerSocketChannel serverSocketChannel;

    private MultipleWebSocketHandler handler;

    private WebSocketServer(final int port) {
        this.port = port;
        this.handler = new MultipleWebSocketHandler();
        this.converter = new FrameDataByteBufferConverterImpl();
    }

    public static WebSocketServer port(final int port) {
        return new WebSocketServer(port);
    }

    public WebSocketServer addWebSocketHandler(final WebSocketHandler handler) {
        this.handler.getHandlers().add(handler);
        return this;
    }

    public WebSocketServer setHandlers(final List<WebSocketHandler> handlers) {
        this.handler.setHandlers(handlers);
        return this;
    }

    public void start() throws IOException, NoSuchAlgorithmException {

        System.out.println("WebSocketServer started on port " + port);

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

                        handleAcceptable(selector);

                    } else if (key.isReadable()) {

                        handleReadable((SocketChannel) key.channel(), (WebSocketImpl) key.attachment());

                    } else if (key.isWritable()) {

                        handleWritable((SocketChannel) key.channel(), (WebSocketImpl) key.attachment());
                    }

                    keyIterator.remove();
                }
            }
        }
    }

    private void handleAcceptable(final Selector selector) throws IOException {
        final SocketChannel channel = serverSocketChannel.accept();
        final WebSocketImpl webSocket = new WebSocketImpl();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, webSocket);
    }

    private void handleReadable(final SocketChannel channel, final WebSocketImpl webSocket) throws IOException, NoSuchAlgorithmException {
        final ByteBuffer buffer = readByteBuffer(channel, webSocket);
        final boolean hasData = (buffer != null) && (buffer.remaining() > 0);
        if (hasData) {
            if (webSocket.isHandshake()) {
                processFrameData(channel, webSocket, buffer);
            } else {
                final String secWebSocketKey = getSecWebSocketKey(buffer);
                handShake(channel, webSocket, secWebSocketKey);
            }
        }
    }

    private void handleWritable(final SocketChannel channel, final WebSocketImpl webSocket) {
        final Queue<FrameData> queue = webSocket.getMessageQueue();
        while (!queue.isEmpty()) {
            try {
                //Take element from queue
                final FrameData frameData = queue.poll();
                final ByteBuffer frameBuffer = converter.convertToByteBuffer(frameData).flip();
                channel.write(frameBuffer);
            } catch (final IOException e) {
                handler.onError(webSocket, e);
            }
        }
    }

    private void handShake(final SocketChannel channel, final WebSocketImpl webSocket, final String secWebSocketKey) throws IOException, NoSuchAlgorithmException {
        if (secWebSocketKey != null) {
            final String response = buildHandshakeResponse(secWebSocketKey);
            final ByteBuffer byteBuffer = ByteBufferUtils.create(response).flip();

            channel.write(byteBuffer);
            webSocket.setHandshake(true);

            System.out.println("===============================");
            System.out.println("WebSocket Handshake");
            System.out.println("Request Sec-WebSocket-Key : " + secWebSocketKey);
            System.out.println("-------------------------------");
            System.out.println("Http Response : ");
            System.out.println(response);

            handler.onConnect(webSocket);
        }
    }

    private String getSecWebSocketKey(final ByteBuffer byteBuffer) {
        final String text = new String(byteBuffer.array(), StandardCharsets.UTF_8);
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
                .append("Sec-WebSocket-Accept: ").append(secWebSocketAccept).append("\r\n\r\n")
                .toString();
    }

    private void processFrameData(final SocketChannel channel, final WebSocketImpl webSocket, final ByteBuffer byteBuffer) {
        try {
            final FrameData frameData = converter.convertToFrameData(byteBuffer);
            if (frameData.getOpcode() == Opcode.CONNECTION_CLOSE) channel.close();
            handler.onMessage(webSocket, frameData);
        } catch (final Throwable e) {
            handler.onError(webSocket, e);
        }
    }

    private ByteBuffer readByteBuffer(final SocketChannel channel, final WebSocketImpl webSocket) {
        ByteBuffer buffer = null;
        try {
            buffer = ByteBufferUtils.read(channel, READ_BUFFER_SIZE).flip();
        } catch (final IOException e) {
            handler.onError(webSocket, e);
        }
        return buffer;
    }

    public void stop() throws IOException {
        serverSocketChannel.close();
        handler.getHandlers().clear();
    }
}
