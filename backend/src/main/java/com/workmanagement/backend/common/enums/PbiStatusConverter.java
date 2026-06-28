package com.workmanagement.backend.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PbiStatusConverter implements AttributeConverter<PbiStatus, String> {

    @Override
    public String convertToDatabaseColumn(PbiStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PbiStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PbiStatus.fromValue(dbData);
    }

}
