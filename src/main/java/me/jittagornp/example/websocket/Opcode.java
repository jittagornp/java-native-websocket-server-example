/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

/**
 * Opcode:  4 bits
 * <p>
 * Defines the interpretation of the "Payload data".  If an unknown
 * opcode is received, the receiving endpoint MUST _Fail the
 * WebSocket Connection_.  The following values are defined.
 * <p>
 * *  %x0 denotes a continuation frame
 * <p>
 * *  %x1 denotes a text frame
 * <p>
 * *  %x2 denotes a binary frame
 * <p>
 * *  %x3-7 are reserved for further non-control frames
 * <p>
 * *  %x8 denotes a connection close
 * <p>
 * *  %x9 denotes a ping
 * <p>
 * *  %xA denotes a pong
 * <p>
 * *  %xB-F are reserved for further control frames
 *
 * @author jitta
 */
public enum Opcode {

    CONTINUATION_FRAME((byte) 0b00000000),
    TEXT_FRAME((byte) 0b00000001),
    BINARY_FRAME((byte) 0b00000010),
    CONNECTION_CLOSE((byte) 0b00001000),
    PING((byte) 0b00001001),
    PONG((byte) 0b00001010);

    private final byte byteValue;

    private Opcode(final byte byteValue) {
        this.byteValue = byteValue;
    }

    public byte getByteValue() {
        return byteValue;
    }

    public static Opcode fromByteValue(final byte byteValue) {
        final Opcode[] opcodes = values();
        for (Opcode opcode : opcodes) {
            if (opcode.getByteValue() == byteValue) {
                return opcode;
            }
        }
        throw new UnsupportedOperationException("Unknown opcode of " + byteValue);
    }
}
