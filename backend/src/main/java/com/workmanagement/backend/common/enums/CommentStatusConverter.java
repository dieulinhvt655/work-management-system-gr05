package com.workmanagement.backend.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CommentStatusConverter implements AttributeConverter<CommentStatus, String> {

    @Override
    public String convertToDatabaseColumn(CommentStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public CommentStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CommentStatus.fromValue(dbData);
    }

}
