package com.workmanagement.backend.common.enums;

public enum SprintStatus {

    PLANNING("planning"),
    ACTIVE("active"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String value;

    SprintStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SprintStatus fromValue(String value) {
        for (SprintStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown SprintStatus: " + value);
    }

}
