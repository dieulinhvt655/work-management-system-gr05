package com.workmanagement.backend.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleScopeConverter implements AttributeConverter<RoleScope, String> {

    @Override
    public String convertToDatabaseColumn(RoleScope attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public RoleScope convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return RoleScope.SYSTEM;
        }
        try {
            return RoleScope.fromValue(dbData.trim());
        } catch (IllegalArgumentException ex) {
            return RoleScope.SYSTEM;
        }
    }

}
