/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author jitta
 */
public class ByteBufferUtils {

    private ByteBufferUtils() {

    }

    public static ByteBuffer create(final String text) {
        final byte[] byteArray = text.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer byteBuffer = ByteBuffer.allocate(byteArray.length);
        byteBuffer.put(byteArray);
        return byteBuffer;
    }

    public static ByteBuffer read(final ReadableByteChannel channel, final int bufferSize) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        final List<ByteBuffer> buffers = new ArrayList<>();
        while (true) {
            //Read data / Write data from channel to byteBuffer
            int status = channel.read(buffer.clear());
            if (status <= 0) {
                break;
            }

            final boolean hasData = buffer.flip().remaining() > 0;
            if (!hasData) {
                break;
            }

            buffers.add(copy(buffer));
        }

        return concat(buffers);
    }

    public static ByteBuffer copy(final ByteBuffer origin) {
        final int realSize = origin.remaining();
        final ByteBuffer copy = ByteBuffer.allocate(realSize);
        copy.put(origin.array(), origin.position(), origin.limit());
        return copy;
    }

    public static ByteBuffer concat(final Collection<ByteBuffer> byteBuffers) {

        if (byteBuffers.isEmpty()) {
            return ByteBuffer.allocate(0);
        }

        if (byteBuffers.size() == 1) {
            return byteBuffers.stream().findFirst().get();
        }

        final int totalSize = byteBuffers.stream()
                .map(ByteBuffer::flip)
                .mapToInt(ByteBuffer::remaining)
                .sum();

        if (totalSize == 0) {
            return ByteBuffer.allocate(0);
        }

        final ByteBuffer concatenated = ByteBuffer.allocate(totalSize);
        byteBuffers.forEach(buffer -> concatenated.put(buffer.duplicate()));
        return concatenated;
    }


}
