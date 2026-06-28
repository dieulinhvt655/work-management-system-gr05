package com.workmanagement.backend.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PbiTypeConverter implements AttributeConverter<PbiType, String> {

    @Override
    public String convertToDatabaseColumn(PbiType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PbiType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PbiType.fromValue(dbData);
    }

}
