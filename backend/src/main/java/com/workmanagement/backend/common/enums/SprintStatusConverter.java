package com.workmanagement.backend.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SprintStatusConverter implements AttributeConverter<SprintStatus, String> {

    @Override
    public String convertToDatabaseColumn(SprintStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public SprintStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SprintStatus.fromValue(dbData);
    }

}
