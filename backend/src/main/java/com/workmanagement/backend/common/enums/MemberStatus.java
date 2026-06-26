package com.workmanagement.backend.common.enums;

public enum MemberStatus {

    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    MemberStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MemberStatus fromValue(String value) {
        for (MemberStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown MemberStatus: " + value);
    }

}
