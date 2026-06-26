package com.workmanagement.backend.common.enums;

public enum RoleScope {

    WORKSPACE("workspace"),
    TEAM("team"),
    PROJECT("project"),
    SYSTEM("system");

    private final String value;

    RoleScope(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RoleScope fromValue(String value) {
        for (RoleScope scope : values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown RoleScope: " + value);
    }

}
