/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author jitta
 */
public interface FrameDataByteBufferConverter {

    List<FrameData> convertToFrameData(final List<ByteBuffer> byteBuffers);

    List<ByteBuffer> covertToByteBuffer(final List<FrameData> frames);

}
