package com.workmanagement.backend.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProjectStatusConverter implements AttributeConverter<ProjectStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProjectStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ProjectStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ProjectStatus.fromValue(dbData);
    }

}
