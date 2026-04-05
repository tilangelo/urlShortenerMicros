package com.example.shortener_core.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class StringToJsonConverter implements AttributeConverter<String, String> {
    
    private static final JsonMapper jsonMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    
    @Override
    public String convertToDatabaseColumn(String attribute) {

        if (attribute == null || attribute.trim().isEmpty()) {
            return null;
        }
        try {
            // Валидация что это валидный json с помощью парса
            jsonMapper.readTree(attribute);
            return attribute;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON for auth_config: " + attribute, e);
        }
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        //
        return dbData;
    }
}
