/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import static java.util.stream.Collectors.toList;

/**
 * @author jitta
 */
public class FrameDataByteBufferConverterImpl implements FrameDataByteBufferConverter {

    private final Random random = new Random();

    @Override
    public List<FrameData> convertToFrameData(final List<ByteBuffer> byteBuffers) {
        return byteBuffers.stream()
                .map(this::convertToFrameData)
                .collect(toList());
    }

    @Override
    public List<ByteBuffer> covertToByteBuffer(final List<FrameData> frames) {
        return frames.stream()
                .map(this::convertToByteBuffer)
                .collect(toList());
    }

    private FrameData convertToFrameData(final ByteBuffer byteBuffer) {

        final byte firstByte = byteBuffer.get();

        //1000 0000
        final byte finBits = (byte) 0b10000000;
        final boolean isFin = (((byte) (firstByte & finBits)) >> 7 & 1) == 1;

        //0100 0000
        final byte rsv1Bits = (byte) 0b01000000;
        final boolean isRSV1 = (((byte) (firstByte & rsv1Bits)) >> 6 & 1) == 1;

        //0010 0000
        final byte rsv2Bits = (byte) 0b00100000;
        final boolean isRSV2 = (((byte) (firstByte & rsv2Bits)) >> 5 & 1) == 1;

        //0001 0000
        final byte rsv3Bits = (byte) 0b00010000;
        final boolean isRSV3 = (((byte) (firstByte & rsv3Bits)) >> 4 & 1) == 1;

        //0000 1111
        final byte opcodeBits = (byte) 0b00001111;
        final byte opcodeByteValue = (byte) (firstByte & opcodeBits);
        final Opcode opcode = Opcode.fromByteValue(opcodeByteValue);

        //==========================================
        final byte secondByte = byteBuffer.get();

        //1000 0000
        final byte maskBits = (byte) 0b10000000;
        final boolean isMask = (((byte) (secondByte & maskBits)) >> 7 & 1) == 1;

        //0111 1111
        final byte payloadLengthBits = (byte) 0b01111111;
        final byte payloadLength = (byte) (secondByte & payloadLengthBits);

        //==========================================
        int payloadDataBufferSize = getPayloadDataBufferSize(payloadLength, byteBuffer);
        final ByteBuffer payloadData = ByteBuffer.allocate(payloadDataBufferSize);

        //==========================================
        if (isMask) {
            final ByteBuffer maskingKey = ByteBuffer.allocate(4);
            maskingKey.put(byteBuffer.get());
            maskingKey.put(byteBuffer.get());
            maskingKey.put(byteBuffer.get());
            maskingKey.put(byteBuffer.get());

            //XOR
            for (int i = 0; byteBuffer.hasRemaining(); i++) {
                final byte encoded = (byte) (byteBuffer.get() ^ maskingKey.get(i % 4));
                payloadData.put(encoded);
            }
        } else {
            payloadData.put(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        }

        return FrameData.builder()
                .fin(isFin)
                .rsv1(isRSV1)
                .rsv2(isRSV2)
                .rsv3(isRSV3)
                .opcode(opcode)
                .mask(isMask)
                .payloadData(payloadData)
                .build();
    }

    private int getPayloadDataBufferSize(final byte payloadLength, final ByteBuffer byteBuffer) {

        if (payloadLength >= 0 && payloadLength <= 125) {
            return payloadLength;
        }

        if (payloadLength == 126) {
            final ByteBuffer extended = ByteBuffer.allocate(2);
            extended.put(byteBuffer.get());
            extended.put(byteBuffer.get());
            return new BigInteger(extended.array()).intValue();
        }

        if (payloadLength == 127) {
            final ByteBuffer extended = ByteBuffer.allocate(8);
            extended.put(byteBuffer.get());
            extended.put(byteBuffer.get());
            extended.put(byteBuffer.get());
            extended.put(byteBuffer.get());
            extended.put(byteBuffer.get());
            extended.put(byteBuffer.get());
            extended.put(byteBuffer.get());
            extended.put(byteBuffer.get());
            return new BigInteger(extended.array()).intValue();
        }

        throw new UnsupportedOperationException("Invalid payload length");
    }

    private ByteBuffer convertToByteBuffer(final FrameData frameData) {

        final ByteBuffer payloadData = frameData.getPayloadData();

        //Change to Read mode
        payloadData.flip();

        //==========================================
        byte firstByte = (byte) 0b00000000;

        //FIN:  1 bit
        if (frameData.isFin()) {
            //OR with 1000 0000
            final byte finBits = (byte) 0b10000000;
            firstByte |= finBits;
        }

        //RSV1, RSV2, RSV3:  1 bit each
        if (frameData.isRSV1()) {
            //OR with 0100 0000
            final byte rsv1Bits = (byte) 0b01000000;
            firstByte |= rsv1Bits;
        }

        if (frameData.isRSV2()) {
            //OR with 0010 0000
            final byte rsv2Bits = (byte) 0b00100000;
            firstByte |= rsv2Bits;
        }

        if (frameData.isRSV3()) {
            //OR with 0001 0000
            final byte rsv3Bits = (byte) 0b00010000;
            firstByte |= rsv3Bits;
        }

        //Opcode:  4 bits
        firstByte |= frameData.getOpcode().getByteValue();

        //==========================================
        //Mask:  1 bit (1000 0000 or 0000 0000)
        final byte maskBits = frameData.isMask() ? (byte) 0b10000000 : (byte) 0b00000000;

        //Payload length:  7 bits, 7+16 bits, or 7+64 bits
        final ByteBuffer payloadLength = buildPayloadLengthByteBuffer(payloadData.remaining(), maskBits);

        //Change to Read mode
        payloadLength.flip();

        //==========================================
        //Allocate frame buffer
        final int maskingKeySize = frameData.isMask() ? 4 : 0;
        final int frameBufferSize = 1 + payloadLength.remaining() + maskingKeySize + payloadData.remaining();
        final ByteBuffer frameBuffer = ByteBuffer.allocate(frameBufferSize);

        frameBuffer.put(firstByte);
        frameBuffer.put(payloadLength.array());

        //==========================================
        //Masking-key:  0 or 4 bytes
        if (frameData.isMask()) {
            final ByteBuffer maskingKey = ByteBuffer.allocate(4);
            maskingKey.putInt(random.nextInt());
            frameBuffer.put(maskingKey.array());
            //XOR
            for (int i = 0; payloadData.hasRemaining(); i++) {
                final byte encoded = (byte) (payloadData.get() ^ maskingKey.get(i % 4));
                frameBuffer.put(encoded);
            }
        } else {
            frameBuffer.put(payloadData.array(), payloadData.position(), payloadData.limit());
        }

        return frameBuffer;
    }

    private ByteBuffer buildPayloadLengthByteBuffer(final int length, final byte maskBits) {

        if (length <= 125) {
            //1 byte = 1 bit + 7 bits
            return buildPayloadLengthAndExtended(
                    length,
                    maskBits,
                    1,
                    (byte) length
            );
        }

        if (length <= 65535) {
            //3 bytes = 1 bit + 7+16 bits
            return buildPayloadLengthAndExtended(
                    length,
                    maskBits,
                    3,
                    (byte) 126
            );
        }

        //9 bytes = 1 bit + 7+64 bits
        return buildPayloadLengthAndExtended(
                length,
                maskBits,
                9,
                (byte) 127
        );
    }

    private ByteBuffer buildPayloadLengthAndExtended(
            final int extendedLength,
            final byte maskBits,
            final int byteCapacity,
            final byte payloadLength
    ) {
        final ByteBuffer buffer = ByteBuffer.allocate(byteCapacity);
        final byte firstByte = (byte) (payloadLength | maskBits);
        buffer.put(firstByte);
        if (byteCapacity > 1) {
            final byte[] extended = convertToByteArray(extendedLength, byteCapacity - 1);
            buffer.put(extended);
        }
        return buffer;
    }

    private byte[] convertToByteArray(final int value, final int byteCapacity) {
        final ByteBuffer buffer = ByteBuffer.allocate(byteCapacity);
        buffer.putInt(value);
        return buffer.array();
    }
}
