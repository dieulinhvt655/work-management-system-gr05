package com.workmanagement.backend.common.enums;

public enum CommonStatus {

    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    CommonStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CommonStatus fromValue(String value) {
        for (CommonStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown CommonStatus: " + value);
    }

}
