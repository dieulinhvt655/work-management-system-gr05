package com.workmanagement.backend.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MemberStatusConverter implements AttributeConverter<MemberStatus, String> {

    @Override
    public String convertToDatabaseColumn(MemberStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public MemberStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MemberStatus.fromValue(dbData);
    }

}
