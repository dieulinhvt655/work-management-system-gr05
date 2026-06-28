package com.workmanagement.backend.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PriorityLevelConverter implements AttributeConverter<PriorityLevel, String> {

    @Override
    public String convertToDatabaseColumn(PriorityLevel attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PriorityLevel convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PriorityLevel.fromValue(dbData);
    }

}
