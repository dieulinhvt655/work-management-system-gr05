package com.workmanagement.backend.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CommonStatusConverter implements AttributeConverter<CommonStatus, String> {

    @Override
    public String convertToDatabaseColumn(CommonStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public CommonStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CommonStatus.fromValue(dbData);
    }

}
