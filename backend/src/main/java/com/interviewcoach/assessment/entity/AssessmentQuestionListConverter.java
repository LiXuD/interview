package com.interviewcoach.assessment.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.AssessmentQuestionDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.List;

@Converter
public class AssessmentQuestionListConverter implements AttributeConverter<List<AssessmentQuestionDto>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<AssessmentQuestionDto> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize assessment questions to JSON", e);
        }
    }

    @Override
    public List<AssessmentQuestionDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize assessment questions from JSON", e);
        }
    }
}
