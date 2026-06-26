package com.workmanagement.backend.common.enums;

public enum PbiStatus {

    NEW("new"),
    READY("ready"),
    IN_SPRINT("in_sprint"),
    COMPLETED("completed"),
    ON_HOLD("on_hold");

    private final String value;

    PbiStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PbiStatus fromValue(String value) {
        for (PbiStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PbiStatus: " + value);
    }

}
