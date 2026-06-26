package com.workmanagement.backend.common.enums;

public enum PbiType {

    FEATURE("feature"),
    BUG("bug"),
    IMPROVEMENT("improvement"),
    TASK("task"),
    OTHER("other");

    private final String value;

    PbiType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PbiType fromValue(String value) {
        for (PbiType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PbiType: " + value);
    }

}
