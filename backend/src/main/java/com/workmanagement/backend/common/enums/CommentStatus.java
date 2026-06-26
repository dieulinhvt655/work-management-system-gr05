package com.workmanagement.backend.common.enums;

public enum CommentStatus {

    ACTIVE("active"),
    EDITED("edited"),
    DELETED("deleted");

    private final String value;

    CommentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CommentStatus fromValue(String value) {
        for (CommentStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown CommentStatus: " + value);
    }

}
