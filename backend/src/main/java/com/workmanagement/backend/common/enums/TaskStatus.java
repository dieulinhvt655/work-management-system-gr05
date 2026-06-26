package com.workmanagement.backend.common.enums;

public enum TaskStatus {

    TO_DO("to_do"),
    IN_PROGRESS("in_progress"),
    REVIEW("review"),
    DONE("done"),
    REOPENED("reopened"),
    CANCELLED("cancelled");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TaskStatus fromValue(String value) {
        for (TaskStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TaskStatus: " + value);
    }

}
