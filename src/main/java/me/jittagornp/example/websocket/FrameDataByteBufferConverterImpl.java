/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * @author jitta
 */
class FrameDataByteBufferConverterImpl implements FrameDataByteBufferConverter {

    //1000 0000
    private static final byte FIN_BITS = (byte) 0b10000000;

    //0100 0000
    private static final byte RSV1_BITS = (byte) 0b01000000;

    //0010 0000
    private static final byte RSV2_BITS = (byte) 0b00100000;

    //0001 0000
    private static final byte RSV3_BITS = (byte) 0b00010000;

    //0000 1111
    private static final byte OPCODE_BITS = (byte) 0b00001111;

    //1000 0000
    private static final byte MASK_BITS = (byte) 0b10000000;

    //0111 1111
    private static final byte PAYLOAD_LENGTH_BITS = (byte) 0b01111111;

    private final Random random = new Random();

    @Override
    public FrameData convertToFrameData(final ByteBuffer byteBuffer) {

        final byte firstByte = byteBuffer.get();

        final boolean isFin = (((byte) (firstByte & FIN_BITS)) >> 7 & 1) == 1;
        final boolean isRSV1 = (((byte) (firstByte & RSV1_BITS)) >> 6 & 1) == 1;
        final boolean isRSV2 = (((byte) (firstByte & RSV2_BITS)) >> 5 & 1) == 1;
        final boolean isRSV3 = (((byte) (firstByte & RSV3_BITS)) >> 4 & 1) == 1;

        final byte opcodeByteValue = (byte) (firstByte & OPCODE_BITS);
        final Opcode opcode = Opcode.fromByteValue(opcodeByteValue);

        //==========================================
        final byte secondByte = byteBuffer.get();

        final boolean isMask = (((byte) (secondByte & MASK_BITS)) >> 7 & 1) == 1;
        final byte payloadLength = (byte) (secondByte & PAYLOAD_LENGTH_BITS);

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

    @Override
    public ByteBuffer convertToByteBuffer(final FrameData frameData) {

        final ByteBuffer payloadData = frameData.getPayloadData().flip();

        //==========================================
        byte firstByte = (byte) 0b00000000;

        //FIN:  1 bit
        if (frameData.isFin()) {
            firstByte |= FIN_BITS;
        }

        //RSV1, RSV2, RSV3:  1 bit each
        if (frameData.isRSV1()) {
            firstByte |= RSV1_BITS;
        }

        if (frameData.isRSV2()) {
            firstByte |= RSV2_BITS;
        }

        if (frameData.isRSV3()) {
            firstByte |= RSV3_BITS;
        }

        //Opcode:  4 bits
        firstByte |= frameData.getOpcode().getByteValue();

        //==========================================
        //Mask:  1 bit (1000 0000 or 0000 0000)
        final byte maskBits = frameData.isMask() ? MASK_BITS : (byte) 0b00000000;

        //Payload length:  7 bits, 7+16 bits, or 7+64 bits
        final ByteBuffer payloadLength = buildPayloadLengthByteBuffer(payloadData.remaining(), maskBits).flip();

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
            final ByteBuffer maskingKey = randomMaskingKey();
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

    private ByteBuffer randomMaskingKey() {
        final ByteBuffer maskingKey = ByteBuffer.allocate(4);
        maskingKey.putInt(random.nextInt());
        return maskingKey;
    }

    private ByteBuffer buildPayloadLengthByteBuffer(final int length, final byte maskBits) {

        if (length >= 0 && length <= 125) {
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
