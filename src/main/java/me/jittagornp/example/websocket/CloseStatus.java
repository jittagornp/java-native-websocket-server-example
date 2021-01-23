/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.example.websocket;

/**
 * https://tools.ietf.org/html/rfc6455#section-7.4.1
 *
 * @author jitta
 */
public enum CloseStatus {

    NORMAL(1000, "Normal close"),
    GOING_AWAY(1001, "Going away"),
    PROTOCOL_ERROR(1002, "Protocol error"),
    REFUSE(1003, "Refuse"),
    NO_STATUS_CODE(1005, "No status code"),
    ABNORMAL_CLOSE(1006, "Abnormal close"),
    NON_UTF8(1007, "Non UTF-8"),
    POLICY_VALIDATION(1008, "Policy validation"),
    TOO_BIG(1009, "Too big"),
    EXTENSION(1010, "Extension"),
    UNEXPECTED_CONDITION(1011, "Unexpected condition"),
    SERVICE_RESTART(1012, "Service restart"),
    TRY_AGAIN_LATER(1013, "Try again later"),
    BAD_GATEWAY(1014, "Bad gateway"),
    TLS_ERROR(1015, "TLS error");

    private final int code;
    private final String reason;

    private CloseStatus(final int code, final String reason) {
        this.code = code;
        this.reason = reason;
    }

    public int getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    public static CloseStatus fromCode(final int code) {
        final CloseStatus[] closeStatuses = values();
        for (CloseStatus closeStatus : closeStatuses) {
            if (closeStatus.getCode() == code) {
                return closeStatus;
            }
        }
        throw new UnsupportedOperationException("Unknown status code");
    }

    @Override
    public String toString() {
        return "CloseStatus{" +
                "code=" + code +
                ", reason='" + reason + '\'' +
                '}';
    }
}
