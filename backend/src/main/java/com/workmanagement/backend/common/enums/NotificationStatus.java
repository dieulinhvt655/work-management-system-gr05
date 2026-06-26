package com.workmanagement.backend.common.enums;

public enum NotificationStatus {

    UNREAD("unread"),
    READ("read");

    private final String value;

    NotificationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NotificationStatus fromValue(String value) {
        for (NotificationStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown NotificationStatus: " + value);
    }

}
