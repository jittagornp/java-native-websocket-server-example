/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

import java.nio.ByteBuffer;

/**
 * https://tools.ietf.org/html/rfc6455#section-5.2
 * <p>
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-------+-+-------------+-------------------------------+
 * |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
 * |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
 * |N|V|V|V|       |S|             |   (if payload len==126/127)   |
 * | |1|2|3|       |K|             |                               |
 * +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
 * |     Extended payload length continued, if payload len == 127  |
 * + - - - - - - - - - - - - - - - +-------------------------------+
 * |                               |Masking-key, if MASK set to 1  |
 * +-------------------------------+-------------------------------+
 * | Masking-key (continued)       |          Payload Data         |
 * +-------------------------------- - - - - - - - - - - - - - - - +
 * :                     Payload Data continued ...                :
 * + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
 * |                     Payload Data continued ...                |
 * +---------------------------------------------------------------+
 *
 * @author jitta
 */
public class FrameData {

    private final boolean isFin;

    private final boolean isRSV1;

    private final boolean isRSV2;

    private final boolean isRSV3;

    private final Opcode opcode;

    private final boolean isMask;

    private final ByteBuffer payloadData;

    public FrameData(
            final boolean isFin,
            final boolean isRSV1,
            final boolean isRSV2,
            final boolean isRSV3,
            final Opcode opcode,
            final boolean isMask,
            final ByteBuffer payloadData
    ) {
        this.isFin = isFin;
        this.isRSV1 = isRSV1;
        this.isRSV2 = isRSV2;
        this.isRSV3 = isRSV3;
        this.opcode = opcode;
        this.isMask = isMask;
        this.payloadData = payloadData;
    }

    public boolean isFin() {
        return isFin;
    }

    public boolean isRSV1() {
        return isRSV1;
    }

    public boolean isRSV2() {
        return isRSV2;
    }

    public boolean isRSV3() {
        return isRSV3;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public boolean isMask() {
        return isMask;
    }

    public ByteBuffer getPayloadData() {
        return payloadData;
    }

    @Override
    public String toString() {
        return "FrameData{" +
                "isFin=" + isFin +
                ", isRSV1=" + isRSV1 +
                ", isRSV2=" + isRSV2 +
                ", isRSV3=" + isRSV3 +
                ", opcode=" + opcode +
                ", isMask=" + isMask +
                ", payloadData=" + payloadData +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean isFin;

        private boolean isRSV1;

        private boolean isRSV2;

        private boolean isRSV3;

        private Opcode opcode;

        private boolean isMask;

        private ByteBuffer payloadData;

        public Builder fin(final boolean fin) {
            isFin = fin;
            return this;
        }

        public Builder rsv1(final boolean rsv1) {
            isRSV1 = rsv1;
            return this;
        }

        public Builder rsv2(final boolean rsv2) {
            isRSV2 = rsv2;
            return this;
        }

        public Builder rsv3(final boolean rsv3) {
            isRSV3 = rsv3;
            return this;
        }

        public Builder opcode(final Opcode opcode) {
            this.opcode = opcode;
            return this;
        }

        public Builder mask(final boolean mask) {
            isMask = mask;
            return this;
        }

        public Builder payloadData(final ByteBuffer payloadData) {
            this.payloadData = payloadData;
            return this;
        }

        public FrameData build() {
            return new FrameData(
                    isFin,
                    isRSV1,
                    isRSV2,
                    isRSV3,
                    opcode,
                    isMask,
                    payloadData
            );
        }
    }
}
