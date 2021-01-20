/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

import java.nio.ByteBuffer;
/**
 * @author jitta
 */
interface FrameDataByteBufferConverter {

    FrameData convertToFrameData(final ByteBuffer byteBuffer);

    ByteBuffer convertToByteBuffer(final FrameData frameData);

}
