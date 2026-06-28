package com.workmanagement.backend.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {

    @Override
    public String convertToDatabaseColumn(UserStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public UserStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return UserStatus.INACTIVE;
        }
        try {
            return UserStatus.fromValue(dbData.trim());
        } catch (IllegalArgumentException ex) {
            return UserStatus.INACTIVE;
        }
    }

}
