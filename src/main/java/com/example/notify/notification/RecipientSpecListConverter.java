package com.example.notify.notification;

import com.example.notify.common.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class RecipientSpecListConverter implements AttributeConverter<List<RecipientSpec>, String> {

    @Override
    public String convertToDatabaseColumn(List<RecipientSpec> attribute) {
        return JsonUtils.write(attribute == null ? List.of() : attribute);
    }

    @Override
    public List<RecipientSpec> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        return JsonUtils.read(dbData, new TypeReference<List<RecipientSpec>>() {
        });
    }
}
