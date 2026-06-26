package com.workmanagement.backend.common.enums;

public enum PriorityLevel {

    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    URGENT("urgent");

    private final String value;

    PriorityLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PriorityLevel fromValue(String value) {
        for (PriorityLevel level : values()) {
            if (level.value.equals(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown PriorityLevel: " + value);
    }

}
