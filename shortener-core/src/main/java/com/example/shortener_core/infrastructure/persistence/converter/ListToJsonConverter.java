package com.example.shortener_core.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter(autoApply = false)
public class ListToJsonConverter implements AttributeConverter<List<String>, String> {
    
    private static final JsonMapper jsonMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        System.out.println("ListToJsonConverter.convertToDatabaseColumn called with: " + attribute);
        if (attribute == null || attribute.isEmpty()) {
            System.out.println("Returning empty array []");
            return "[]";
        }
        try {
            String json = jsonMapper.writeValueAsString(attribute);
            System.out.println("Converted to JSON: " + json);
            return json;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting list to JSON", e);
        }
    }
    
    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return List.of();
        }
        try {
            return jsonMapper.readValue(dbData, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON to list: " + dbData, e);
        }
    }
}
